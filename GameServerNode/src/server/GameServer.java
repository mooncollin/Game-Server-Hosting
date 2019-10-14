package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import javax.websocket.Session;

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
	
	public static void Setup(Connection connection)
	{
		
	}
	
	public static final int WAIT_TIME = 500;
	public static final String SERVER_ON_MESSAGE = "<on>";
	public static final String SERVER_OFF_MESSAGE = "<off>";
	
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
	private final List<OutputStream> outputConnectors;

	public GameServer(String name, File folderLocation, File fileName)
	{
		this(name, folderLocation, fileName, DEFAULT_LOG_MAXIMUM_LENGTH);
	}
	
	public GameServer(String name, File folderLocation, File fileName, int logSize)
	{
		runningStateChangeNotifiers = Collections.synchronizedList(new LinkedList<Object>());;
		triggerHandlers = Collections.synchronizedList(new LinkedList<TriggerHandler>());
		timerTasks = Collections.synchronizedList(new LinkedList<TimerTask>());
		outputConnectors = new LinkedList<OutputStream>();
		timer = new Timer();
		setName(name);
		setLogSize(logSize);
		setFolderLocation(folderLocation);
		setExecutableName(fileName);
		commandHandler = generateCommandHandler();
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
	
	public List<OutputStream> getOutputConnectors()
	{
		return outputConnectors;
	}
	
	public Runnable getOutputRunnable(Session s)
	{
		return new OutputRunnable(s, this);
	}
	
	public Runnable getServerRunningStatusRunnable(Session s)
	{
		return new ServerRunningStatusRunnable(s, this);
	}
	
	abstract public boolean stopServer();
	abstract public boolean startServer();
	abstract public boolean writeToServer(String out);
	
	private class OutputRunnable implements Runnable
	{
		private Session session;
		private GameServer server;
		private PipedOutputStream serverOut;
		private PipedInputStream pipedIn;
		private BufferedReader clientIn;
		private Object notifier;
		
		public OutputRunnable(Session s, GameServer server)
		{
			this.session = s;
			this.server = server;
			resetPipes();
			server.getOutputConnectors().add(serverOut);
			notifier = server.getRunningStateNotifier();
		}
		
		private void resetPipes()
		{
			if(serverOut != null)
			{
				try
				{
					serverOut.close();
					server.getOutputConnectors().remove(serverOut);
				} catch (IOException e)
				{
					e.printStackTrace();
					return;
				}
			}
			if(pipedIn != null)
			{
				try
				{
					pipedIn.close();
					clientIn.close();
				} catch (IOException e)
				{
					e.printStackTrace();
					return;
				}
			}
			serverOut = new PipedOutputStream();
			pipedIn = new PipedInputStream();
			try
			{
				serverOut.connect(pipedIn);
			} catch (IOException e)
			{
				e.printStackTrace();
				return;
			}
			clientIn = new BufferedReader(new InputStreamReader(pipedIn));
			server.getOutputConnectors().add(serverOut);
		}
		
		public void run()
		{
			String lastLine = null;
			while(session.isOpen() && !Thread.currentThread().isInterrupted())
			{
				try
				{
					String output;
					if(!session.isOpen() || Thread.currentThread().isInterrupted())
					{
						break;
					}
					
					while(clientIn.ready())
					{
						synchronized(clientIn)
						{
							output = clientIn.readLine();
						}
						if(output == null || lastLine != null && lastLine.equals(output))
						{
							continue;
						}
						lastLine = output;
						session.getBasicRemote().sendText(output + "\r\n");
					}
					
					Thread.sleep(WAIT_TIME);
				} catch (IOException | IllegalStateException | InterruptedException e)
				{
					StartUpApplication.LOGGER.log(Level.WARNING, String.format("Failed to send output data to server '%s':\n%s", server.getName(), e.getMessage()));
					break;
				}
			}
			
			List<OutputStream> serverConnectors = server.getOutputConnectors();
			
			synchronized(serverConnectors)
			{
				if(!serverConnectors.remove(serverOut))
				{
					StartUpApplication.LOGGER.log(Level.WARNING, String.format("Unable to remove output connector from server '%s'", server.getName()));
				}
			}
			server.removeRunningStateNotifier(notifier);
			
			try
			{
				serverOut.close();
				clientIn.close();
			} catch (IOException e)
			{
			}
		}
	}
	
	private class ServerRunningStatusRunnable implements Runnable
	{
		private Session session;
		private GameServer server;
		private Object notifier;
		
		public ServerRunningStatusRunnable(Session s, GameServer server)
		{
			this.session = s;
			this.server = server;
			this.notifier = server.getRunningStateNotifier();
		}
		
		public void run()
		{
			Boolean lastSent = null;
			while(session.isOpen() && !Thread.currentThread().isInterrupted())
			{
				try
				{
					if(lastSent != null)
					{
						if(lastSent.booleanValue() == server.isRunning())
						{
							Thread.sleep(500);
							continue;
						}
						synchronized (notifier)
						{
							notifier.wait(WAIT_TIME);
						}
						if(!session.isOpen() || Thread.currentThread().isInterrupted())
						{
							break;
						}
					}
				}
				catch(InterruptedException e)
				{
					break;
				}
				
				lastSent = server.isRunning();
				String text = lastSent ? SERVER_ON_MESSAGE : SERVER_OFF_MESSAGE;
				try
				{
					session.getBasicRemote().sendText(text);
				} catch (IOException e)
				{
					StartUpApplication.LOGGER.log(Level.WARNING, String.format("Failed to send running data from server '%s':\n%s", server.getName(), e.getMessage()));
					break;
				}
			}
			
			server.removeRunningStateNotifier(this.notifier);
		}
	}
}
