package server;

import java.io.File;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import api.minecraft.MinecraftServer;
import model.Database;
import models.GameServerTable;
import models.NodeTable;
import models.TriggersTable;
import nodemain.StartUpApplication;

import java.util.Collections;

abstract public class GameServer
{
	public static final int DEFAULT_LOG_MAXIMUM_LENGTH = 1_000_000;

	public static final Map<String, Class<? extends GameServer>> PROPERTY_NAMES_TO_TYPE = 
		Map.ofEntries(
				Map.entry("minecraft", MinecraftServer.class)
		);
	
	protected Process process;
	protected String log;
	private int logSize;
	private File folderLocation;
	private File executableName;
	private final List<TriggerHandler> triggerHandlers;
	private final Timer timer;
	private final List<TimerTask> timerTasks;
	private final List<OutputStream> outputConnectors;
	private final List<Object> runningStateChangeNotifiers;
	private final List<Object> outputNotifiers;
	
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
		runningStateChangeNotifiers = new LinkedList<Object>();
		outputNotifiers = new LinkedList<Object>();
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
	
	public Object getOutputNotifier()
	{
		var notifier = new Object();
		synchronized(outputNotifiers)
		{
			outputNotifiers.add(notifier);
		}
		
		return notifier;
	}
	
	public boolean removeOutputNotifier(Object n)
	{
		synchronized(outputNotifiers)
		{
			return outputNotifiers.remove(n);
		}
	}
	
	public Object getRunningStateNotifier()
	{
		var stateN = new Object();
		synchronized(runningStateChangeNotifiers)
		{
			runningStateChangeNotifiers.add(stateN);
		}

		return stateN;
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
	
	public boolean removeRunningStateNotifier(Object n)
	{
		synchronized(runningStateChangeNotifiers)
		{
			return runningStateChangeNotifiers.remove(n);
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
			notifyRunningNotifiers();
		} catch (InterruptedException e)
		{
		}
		
		StartUpApplication.LOGGER.log(Level.WARNING, "Server forced to stop");
		return !process.isAlive();
	}
	
	public boolean isRunning()
	{
		return process != null && process.isAlive();
	}
	
	protected void notifyRunningNotifiers()
	{
		synchronized(runningStateChangeNotifiers)
		{
			runningStateChangeNotifiers.parallelStream()
			   .forEach(notifier -> {
				   synchronized (notifier) {
					   notifier.notifyAll();
				   }
			   });
		}
	}
	
	protected void notifyOutputNotifiers()
	{
		synchronized (outputNotifiers)
		{
			outputNotifiers.parallelStream()
				.forEach(notifier -> {
					synchronized (notifier) {
						notifier.notifyAll();
					}
				});
		}
	}
	
	abstract public boolean stopServer();
	abstract public boolean startServer();
	abstract public boolean writeToServer(String out);
	
	protected List<OutputStream> getOutputConnectors()
	{
		return outputConnectors;
	}
	
	protected List<Object> getRunningStateChangeNotifiers()
	{
		return runningStateChangeNotifiers;
	}
	
	protected List<Object> getOutputNotifiers()
	{
		return outputNotifiers;
	}
}
