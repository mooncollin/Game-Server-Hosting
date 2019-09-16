package api.minecraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import main.StartUpApplication;
import server.CommandHandler;
import server.GameServer;
import server.TriggerHandler;
import server.TriggerHandlerCondition;
import server.TriggerHandlerCondition.TriggerHandlerConditionType;
import utils.BoundedCircularList;

public class MinecraftServer extends GameServer
{
	public static final int MINIMUM_HEAP_SIZE = 1024;
	public static final int DEFAULT_LAST_READ_MAXIMUM_SIZE = 500;
	public static final String SERVER_PROPERTIES_FILE_NAME = "server.properties";
	
	public static final Map<String, Object> MINECRAFT_PROPERTIES = Map.ofEntries
	(
		Map.entry("allow-flight", false),
		Map.entry("allow-nether", true),
		Map.entry("broadcast-rcon-to-ops", true),
		Map.entry("difficulty", 1),
		Map.entry("enable-command-block", false),
		Map.entry("enable-query", false),
		Map.entry("enable-rcon", false),
		Map.entry("force-gamemode", false),
		Map.entry("gamemode", 0),
		Map.entry("generate-structures", true),
		Map.entry("generator-settings", ""),
		Map.entry("hardcore", false),
		Map.entry("level-name", "world"),
		Map.entry("level-seed", ""),
		Map.entry("level-type", "DEFAULT"),
		Map.entry("max-build-height", 256),
		Map.entry("max-players", 20),
		Map.entry("max-tick-time", 60000),
		Map.entry("max-world-size", 29999984),
		Map.entry("motd", "A Minecraft Server"),
		Map.entry("network-compression-threshold", 256),
		Map.entry("online-mode", true),
		Map.entry("op-permission-level", 4),
		Map.entry("player-idle-timeout", 0),
		Map.entry("prevent-proxy-connections", false),
		Map.entry("pvp", true),
		Map.entry("query.port", 25565),
		Map.entry("rcon.password", ""),
		Map.entry("resource-pack", ""),
		Map.entry("resource-pack-sha1", ""),
		Map.entry("server-ip", ""),
		Map.entry("server-port", 25565),
		Map.entry("snooper-enabled", true),
		Map.entry("spawn-animals", true),
		Map.entry("spawn-monsters", true),
		Map.entry("spawn-npcs", true),
		Map.entry("spawn-protection", 16),
		Map.entry("use-native-transport", true),
		Map.entry("view-distance", 10),
		Map.entry("white-list", false),
		Map.entry("enforce-whitelist", false)
	);
	
	private List<OutputStream> outputConnectors;
	private boolean autoRestart;
	private boolean expectedShutdown;
	private int maximumHeapSize;
	private String arguments;
	private BoundedCircularList<String> lastReadBounded;
	private ExecutorService execService;
	private final ProcessBuilder processBuilder;
	private final Runnable inputTask = new Runnable()
	{
		public void run()
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			while(isRunning() && !Thread.currentThread().isInterrupted())
			{
				String lineRead;
				try
				{
					lineRead = reader.readLine();
				} catch (IOException e1)
				{
					StartUpApplication.LOGGER.log(Level.SEVERE, String.format("Reading from '%s' caused an error:\n%s", getName(), e1.getMessage()));
					break;
				}
				if(lineRead == null || Thread.currentThread().isInterrupted())
				{
					break;
				}
				final String lineTerminated = lineRead + "\r\n";
				List<TriggerHandler> triggers = getTriggerHandlers();
				synchronized(triggers)
				{
					for(TriggerHandler trigger : triggers)
					{
						if(trigger instanceof TriggerHandlerCondition)
						{
							@SuppressWarnings("unchecked")
							TriggerHandlerCondition<String> condition = (TriggerHandlerCondition<String>) trigger;
							if(condition.getType().equals(TriggerHandlerConditionType.OUTPUT))
							{
								condition.trigger(lineTerminated);
							}
						}
					}
				}
				outputConnectors.removeIf(stream -> {
					try
					{
						synchronized(stream)
						{
							stream.write(lineTerminated.getBytes());
						}
					} catch (IOException e)
					{
						StartUpApplication.LOGGER.log(Level.INFO, String.format("Minecraft output stream ended:\n%s", e.getMessage()));
						return true;
					}
					return false;
				});
				synchronized(lastReadBounded)
				{
					lastReadBounded.add(lineTerminated);
				}
				log += lineTerminated;
				if(log.length() > getLogSize())
				{
					log = log.substring(getLogSize()/4);
				}
			}
		}
	};
	
	public MinecraftServer(String name, File folderLocation, File fileName, int maximumHeapSize, String arguments)
	{
		this(name, folderLocation, fileName, maximumHeapSize, GameServer.DEFAULT_LOG_MAXIMUM_LENGTH, arguments);
	}
	
	public MinecraftServer(String name, File folderLocation, File fileName, int maximumHeapSize, int logSize, String arguments)
	{
		super(name, folderLocation, fileName, logSize);
		setMaximumHeapSize(maximumHeapSize);
		setArguments(arguments);
		execService = Executors.newSingleThreadExecutor();
//		outputConnectors = Collections.synchronizedList(new LinkedList<OutputStream>());
		outputConnectors = new LinkedList<OutputStream>();
		lastReadBounded = new BoundedCircularList<String>(DEFAULT_LAST_READ_MAXIMUM_SIZE);
		
		setProperties(getProperties());
		processBuilder = new ProcessBuilder();
		processBuilder.redirectErrorStream(true);
	}
	
	public List<OutputStream> getOutputConnectors()
	{
		return outputConnectors;
	}
	
	public void setMaximumHeapSize(int maximumHeapSize)
	{
		if(maximumHeapSize < MINIMUM_HEAP_SIZE)
		{
			throw new IllegalArgumentException("Heap Size must be greater than " + MINIMUM_HEAP_SIZE);
		}
		
		this.maximumHeapSize = maximumHeapSize;
	}
	
	public void setArguments(String arguments)
	{
		this.arguments = Objects.requireNonNullElse(arguments, "");
	}
	
	synchronized public boolean stopServer()
	{
		if(!isRunning())
		{
			return true;
		}
		
		writeToServer("stop");
		
		try
		{
			expectedShutdown = true;
			boolean died = process.waitFor(7, TimeUnit.SECONDS);
			if(!died)
			{
				forceStopServer();
			}
			execService.shutdownNow();
			execService.awaitTermination(5, TimeUnit.SECONDS);
			execService = Executors.newSingleThreadExecutor();
			notifyRunningNotifiers();
			return died;
		} catch (InterruptedException e)
		{
			return false;
		}
	}
	
	@Override
	public synchronized boolean forceStopServer()
	{
		expectedShutdown = true;
		return super.forceStopServer();
	}
	
	synchronized public boolean startServer()
	{
		if(!isRunning())
		{
			try
			{
				processBuilder.directory(getFolderLocation());
				processBuilder.command(getRunCommand());
				process = processBuilder.start();
				notifyRunningNotifiers();
				expectedShutdown = false;
				process.onExit().thenAcceptAsync(p -> {
					notifyRunningNotifiers();
					if(!expectedShutdown && autoRestart())
					{
						try
						{
							Thread.sleep(5000);
							startServer();
						} catch (InterruptedException e)
						{
						}
					}
				});
				lastReadBounded.clear();
				log = "";
				execService.submit(inputTask);
			}
			catch(IOException e)
			{
				return false;
			}
			
			return true;
		}
		
		return false;
	}
	
	synchronized public boolean writeToServer(String out)
	{
		if(isRunning())
		{
			OutputStream outStream = process.getOutputStream();
			try
			{
				outStream.write(out.getBytes());
				outStream.write(System.lineSeparator().getBytes());
				outStream.flush();
				return true;
			}
			catch(IOException e)
			{
				StartUpApplication.LOGGER.log(Level.WARNING, String.format("Failed to write to server '%s':\n%s", getName(), e.getMessage()));
			}
		}
		
		return false;
	}
	
	public String[] getRunCommand()
	{
		List<String> command = new LinkedList<String>();
		command.add(StartUpApplication.JAVA8);
		command.add(String.format("-Xmx%dm", maximumHeapSize));
		command.add(String.format("-Xms%dm", maximumHeapSize));
		for(String extra : arguments.split(" "))
		{
			command.add(extra);
		}
		command.add("-jar");
		command.add(String.format("\"%s\"", getExecutableName().getAbsolutePath()));
		command.add("nogui");
		String[] fullCommand = command.toArray(new String[] {});
		System.out.printf("Running command: %s\n", String.join(" ", fullCommand));
		return fullCommand;
	}
	
	public BoundedCircularList<String> getLastRead()
	{
		return lastReadBounded;
	}
	
	public Properties getProperties()
	{
		Properties properties = new Properties();
		File propertiesFile = getFolderLocation().toPath().resolve(Path.of(SERVER_PROPERTIES_FILE_NAME)).toFile();
		if(propertiesFile.exists())
		{
			try(FileInputStream s = new FileInputStream(propertiesFile))
			{
				properties.load(s);
			} catch (IOException e)
			{
				e.printStackTrace();
				return null;
			}
		}
		
		return properties;
	}
	
	public boolean setProperties(Properties p)
	{
		File propertiesFile = getFolderLocation().toPath().resolve(Path.of(SERVER_PROPERTIES_FILE_NAME)).toFile();
		Properties properties = new Properties();
		for(String prop : MINECRAFT_PROPERTIES.keySet())
		{
			properties.setProperty(prop, String.valueOf(MINECRAFT_PROPERTIES.get(prop)));
		}
		
		if(p != null)
		{
			for(String name : p.stringPropertyNames())
			{
				properties.setProperty(name, p.getProperty(name));
			}
		}
		
		try(FileOutputStream o = new FileOutputStream(propertiesFile))
		{
			properties.store(o, String.format("Minecraft server properties"));
		} catch (IOException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, String.format("Unable to write to server '%s' properties file:\n%s", getName(), e.getMessage()));
			return false;
		}
		
		return true;
	}
	
	@Override
	protected CommandHandler generateCommandHandler()
	{
		return new MinecraftServerCommandHandler(this);
	}
	
	public boolean autoRestart()
	{
		return autoRestart;
	}
	
	public void autoRestart(boolean value)
	{	
		autoRestart = value;
	}
	
	public String getArguments()
	{
		return arguments;
	}
}
