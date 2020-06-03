package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import model.Database;
import models.GameModuleTable;
import models.GameServerTable;
import models.NodeTable;
import models.TriggersTable;
import nodemain.StartUpApplication;

import java.util.Collections;

abstract public class GameServer
{
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
	private File folderLocation;
	private File executableFile;
	private final List<TriggerHandler> triggerHandlers;
	private final Timer timer;
	private final List<TimerTask> timerTasks;
	private final List<OutputStream> outputConnectors;
	private final Object runningStateNotifier = new Object();
	
	public final static void setup(Database db)
	{
		var triggersTable = new TriggersTable();
		var nodeTable = new NodeTable();
		var gameserverTable = new GameServerTable();
		var gametypesTable = new GameModuleTable();
		
		try
		{
			nodeTable.createTable(db);
			gameserverTable.createTable(db);
			triggersTable.createTable(db);
			gametypesTable.createTable(db);
		} catch (SQLException e)
		{
			throw new RuntimeException(String.format("Error Creating tables: %s", e.getMessage()));
		}
	}
	
	public GameServer(File folderLocation, File fileName)
	{
		outputConnectors = new LinkedList<OutputStream>();
		triggerHandlers = Collections.synchronizedList(new LinkedList<TriggerHandler>());
		timerTasks = Collections.synchronizedList(new LinkedList<TimerTask>());
		timer = new Timer();
		setFolderLocation(folderLocation);
		setExecutableFile(fileName);
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
		return getGameServerOptions().getCommandHandler();
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
	
	public void setExecutableFile(File fileName)
	{	
		this.executableFile = Objects.requireNonNull(fileName, "File name cannot be null");
	}
	
	public File getFolderLocation()
	{
		return folderLocation;
	}
	
	public File getExecutableFile()
	{
		return executableFile;
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
	
	abstract public String getLog();
	abstract public boolean stopServer() throws IOException;
	abstract public boolean startServer() throws IOException;
	abstract public boolean writeToServer(Reader in) throws IOException;
	abstract public GameServerOptions<? extends GameServer> getGameServerOptions();
	
	public boolean writeToServer(String in) throws IOException
	{
		return writeToServer(new StringReader(in));
	}
	
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
		
		try(var ip = new BufferedReader(new InputStreamReader(IP_ADDRESS_FINDER.openStream())))
		{
			return ip.readLine();
		} catch (IOException e)
		{
			return null;
		}
	}
	
	protected List<OutputStream> getOutputConnectors()
	{
		return outputConnectors;
	}	
}