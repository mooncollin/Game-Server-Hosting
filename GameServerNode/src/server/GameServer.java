package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import model.Database;
import models.GameServerTable;
import models.NodeTable;
import models.TriggersTable;
import nodemain.StartUpApplication;

import java.util.Collections;

abstract public class GameServer
{
	public static final int DEFAULT_LOG_MAXIMUM_LENGTH = 1_000_000;
	private static final URL IP_ADDRESS_FINDER;
	
	static
	{
		URL temp;
		try
		{
			temp = new URL("http://checkip.amazonaws.com");
		} catch (MalformedURLException e)
		{
			temp = null;
			
		}
		
		IP_ADDRESS_FINDER = temp;
	}
	
	protected Process process;
	protected String log;
	private int logSize;
	private File folderLocation;
	private File executableName;
	private final List<TriggerHandler> triggerHandlers;
	private final Timer timer;
	private final List<TimerTask> timerTasks;
	private final List<OutputStream> outputConnectors;
	private final Object runningStateNotifier = new Object();
	
	public static void setup(Database db)
	{
		var triggersTable = new TriggersTable();
		var nodeTable = new NodeTable();
		var gameserverTable = new GameServerTable();
		
		try
		{
			nodeTable.createTable(db);
			gameserverTable.createTable(db);
			triggersTable.createTable(db);
		} catch (SQLException e)
		{
			throw new RuntimeException(String.format("Error Creating tables: %s", e.getMessage()));
		}
	}

	public GameServer(File folderLocation, File fileName)
	{
		this(folderLocation, fileName, DEFAULT_LOG_MAXIMUM_LENGTH);
	}
	
	public GameServer(File folderLocation, File fileName, int logSize)
	{
		outputConnectors = new LinkedList<OutputStream>();
		triggerHandlers = Collections.synchronizedList(new LinkedList<TriggerHandler>());
		timerTasks = Collections.synchronizedList(new LinkedList<TimerTask>());
		timer = new Timer();
		setLogSize(logSize);
		setFolderLocation(folderLocation);
		setExecutableName(fileName);
	}
	
	public Timer getTriggerTimer()
	{
		return timer;
	}
	
	public List<TimerTask> getTimerTasks()
	{
		return timerTasks;
	}
	
	public List<TriggerHandler> getTriggerHandlers()
	{
		return triggerHandlers;
	}
	
	public CommandHandler<? extends GameServer> getCommandHandler()
	{
		return new GameServerCommandHandler<GameServer>(this);
	}
	
	public void registerOutputConnector(OutputStream s)
	{
		synchronized (outputConnectors)
		{
			outputConnectors.add(s);
		}
	}
	
	public boolean removeOutputConnector(OutputStream s)
	{
		synchronized (outputConnectors)
		{
			return outputConnectors.remove(s);
		}
	}
	
	public void setFolderLocation(File folderLocation)
	{	
		this.folderLocation = Objects.requireNonNull(folderLocation, "Folder location cannot be null");
	}
	
	public void setExecutableName(File fileName)
	{	
		this.executableName = Objects.requireNonNull(fileName, "File name cannot be null");
	}
	
	public void setLogSize(int logSize)
	{
		if(logSize < 1)
		{
			throw new IllegalArgumentException("Log size must be 1 or greater");
		}
		
		this.logSize = logSize;
	}
	
	public File getFolderLocation()
	{
		return folderLocation;
	}
	
	public File getExecutableName()
	{
		return executableName;
	}
	
	public int getLogSize()
	{
		return logSize;
	}
	
	public String getLog()
	{
		return log;
	}
	
	synchronized public boolean forceStopServer()
	{
		if(process != null)
		{
			process.destroyForcibly();
		}
		
		try
		{
			process.waitFor();
		} catch (InterruptedException e)
		{
		}
		
		var result = !process.isAlive();
		
		if(result)
		{
			notifyRunningNotifiers();
			StartUpApplication.LOGGER.warn("Server forced to stop");
		}
		
		return result;
	}
	
	public boolean isRunning()
	{
		return process != null && process.isAlive();
	}
	
	public Object getRunningStateNotifier()
	{
		return runningStateNotifier;
	}
	
	protected void notifyRunningNotifiers()
	{
		synchronized(runningStateNotifier)
		{
			runningStateNotifier.notifyAll();
		}
	}
	
	abstract public boolean stopServer();
	abstract public boolean startServer() throws IOException;
	abstract public boolean writeToServer(String out);
	
	public boolean restartServer() throws IOException
	{
		var stopping = stopServer();
		if(!stopping)
		{
			return false;
		}
		var starting = startServer();
		return starting;
	}
	
	public String getPublicIPAddress()
	{
		if(IP_ADDRESS_FINDER == null)
		{
			return null;
		}
		
		try
		{
			var ip = new BufferedReader(new InputStreamReader(IP_ADDRESS_FINDER.openStream()));
			return ip.readLine();
		} catch (IOException e)
		{
			return null;
		}
	}
	
	abstract public String getServerType();
	
	protected List<OutputStream> getOutputConnectors()
	{
		return outputConnectors;
	}
}
