package server;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import api.minecraft.MinecraftServer;
import main.StartUpApplication;

import java.util.Collections;

abstract public class GameServer
{
	public static final int DEFAULT_LOG_MAXIMUM_LENGTH = 1_000_000;

	public static final Map<String, Class<? extends GameServer>> PROPERTY_NAMES_TO_TYPE = 
		Map.ofEntries(
				Map.entry("minecraft", MinecraftServer.class)
		);
	
	private List<Object> runningStateChangeNotifiers;
	private String name;
	protected Process process;
	protected String log;
	private int logSize;
	private File folderLocation;
	private File executableName;
	private final CommandHandler commandHandler;
	private final List<TriggerHandler> triggerHandlers;
	private final Timer timer;
	private final List<TimerTask> timerTasks;
	
	public GameServer(String name, File folderLocation, File fileName)
	{
		this(name, folderLocation, fileName, DEFAULT_LOG_MAXIMUM_LENGTH);
	}
	
	public GameServer(String name, File folderLocation, File fileName, int logSize)
	{
		runningStateChangeNotifiers = Collections.synchronizedList(new LinkedList<Object>());;
		triggerHandlers = Collections.synchronizedList(new LinkedList<TriggerHandler>());
		timerTasks = Collections.synchronizedList(new LinkedList<TimerTask>());
		setName(name);
		setLogSize(logSize);
		setFolderLocation(folderLocation);
		setExecutableName(fileName);
		commandHandler = generateCommandHandler();
		timer = new Timer();
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
	
	protected CommandHandler generateCommandHandler()
	{
		return new GameServerCommandHandler(this);
	}
	
	public CommandHandler getCommandHandler()
	{
		return commandHandler;
	}
	
	public Object getRunningStateNotifier()
	{
		Object stateN = new Object();
		runningStateChangeNotifiers.add(stateN);			

		return stateN;
	}
	
	public boolean removeRunningStateNotifier(Object n)
	{
		return runningStateChangeNotifiers.remove(n);
	}
	
	public void setFolderLocation(File folderLocation)
	{	
		this.folderLocation = Objects.requireNonNull(folderLocation, "Folder location cannot be null");
	}
	
	public void setExecutableName(File fileName)
	{	
		this.executableName = Objects.requireNonNull(fileName, "File name cannot be null");
	}
	
	public void setName(String name)
	{	
		this.name = Objects.requireNonNull(name, "Name cannot be null");
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
	
	public String getName()
	{
		return name;
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
		
		StartUpApplication.LOGGER.log(Level.WARNING, String.format("Server '%s' forced to stop", getName()));
		return !process.isAlive();
	}
	
	public boolean isRunning()
	{
		return process != null && process.isAlive();
	}
	
	protected void notifyRunningNotifiers()
	{
		synchronized (runningStateChangeNotifiers)
		{
			for(Object notifier : runningStateChangeNotifiers)
			{
				if(notifier != null)
				{
					synchronized (notifier)
					{
						notifier.notifyAll();
					}
				}
			}			
		}
	}
	
	abstract public boolean stopServer();
	abstract public boolean startServer();
	abstract public boolean writeToServer(String out);
}
