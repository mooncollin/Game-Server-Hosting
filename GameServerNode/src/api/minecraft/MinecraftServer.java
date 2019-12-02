package api.minecraft;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import model.Database;
import models.MinecraftServerTable;
import nodemain.NodeProperties;
import nodemain.StartUpApplication;
import server.CommandHandler;
import server.GameServer;
import server.TriggerHandlerCondition;
import server.TriggerHandlerCondition.TriggerHandlerConditionType;
import utils.BoundedCircularList;

public class MinecraftServer extends GameServer
{
	public static final int MINIMUM_HEAP_SIZE = 1024;
	public static final int HEAP_STEP = 1024;
	public static final int DEFAULT_LAST_READ_MAXIMUM_SIZE = 500;
	public static final String SERVER_PROPERTIES_FILE_NAME = "server.properties";
	public static final String MINIMUM_HEAP_ARGUMENT = "-Xms%dm";
	public static final String MAXIMUM_HEAP_ARGUMENT = "-Xmx%dm";
	
	public static final String RAM_AMOUNT_PARAMETER = "ramAmount";
	public static final String RESTART_PARAMETER = "restartsUnexpected";
	public static final String ARGUMENTS_PARAMETER = "arguments";
	public static final String SERVER_COMMAND_PARAMETER = "serverCommand";
	
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
	
	private boolean autoRestart;
	private boolean expectedShutdown;
	private int maximumHeapSize;
	private String arguments;
	private BoundedCircularList<String> lastReadBounded;
	private ExecutorService execService;
	private final ProcessBuilder processBuilder;
	private final Runnable inputTask = new Runnable()
	{
		@SuppressWarnings("unchecked")
		public void run()
		{
			var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			while(isRunning() && !Thread.currentThread().isInterrupted())
			{
				String lineRead;
				try
				{
					lineRead = reader.readLine();
				} catch (IOException e1)
				{
					StartUpApplication.LOGGER.log(Level.SEVERE, String.format("Reading from MinecraftServer caused an error:\n%s", e1.getMessage()));
					break;
				}
				
				if(lineRead == null || Thread.currentThread().isInterrupted())
				{
					break;
				}
				
				final var lineTerminated = lineRead + "\r\n";
				var triggers = getTriggerHandlers();

				triggers.parallelStream()
					.filter(trigger -> trigger instanceof TriggerHandlerCondition)
					.map(trigger -> (TriggerHandlerCondition<String>) trigger)
					.filter(condition -> condition.getType().equals(TriggerHandlerConditionType.OUTPUT))
					.forEach(condition -> condition.trigger(lineRead));
					
				var streamsToRemove = new LinkedList<OutputStream>();
				
				var outputConnectors = getOutputConnectors();
				
				synchronized (outputConnectors)
				{
					outputConnectors.parallelStream()
						.forEach(stream -> {
							synchronized (stream)
							{
								try
								{
									stream.write(lineTerminated.getBytes());
								} catch (IOException e)
								{
									streamsToRemove.add(stream);
								}
							}
						});
					
					outputConnectors.removeAll(streamsToRemove);
				}
				
				synchronized(lastReadBounded)
				{
					lastReadBounded.add(lineTerminated);
				}
				
				log += lineTerminated;
				if(log.length() > getLogSize())
				{
					log = log.substring(getLogSize()/4);
				}
				
				notifyOutputNotifiers();
			}
		}
	};
	
	public static void setup(Database db)
	{
		try
		{
			new MinecraftServerTable().createTable(db);
		}
		catch(SQLException e)
		{
			throw new RuntimeException(String.format("Error Creating tables: %s", e.getMessage()));
		}
	}
	
	public MinecraftServer(File folderLocation, File fileName, int maximumHeapSize, String arguments)
	{
		this(folderLocation, fileName, maximumHeapSize, GameServer.DEFAULT_LOG_MAXIMUM_LENGTH, arguments);
	}
	
	public MinecraftServer(File folderLocation, File fileName, int maximumHeapSize, int logSize, String arguments)
	{
		super(folderLocation, fileName, logSize);
		setMaximumHeapSize(maximumHeapSize);
		setArguments(arguments);
		execService = Executors.newSingleThreadExecutor();
		lastReadBounded = new BoundedCircularList<String>(DEFAULT_LAST_READ_MAXIMUM_SIZE);
		
		setProperties(getProperties());
		processBuilder = new ProcessBuilder();
		processBuilder.redirectErrorStream(true);
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
	
	@Override
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
			var died = process.waitFor(7, TimeUnit.SECONDS);
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
	
	@Override
	synchronized public boolean startServer()
	{
		if(!isRunning())
		{
			processBuilder.directory(getFolderLocation());
			processBuilder.command(getRunCommand());
			
			try
			{
				process = processBuilder.start();				
			}
			catch(IOException e)
			{
				return false;
			}
			
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
			return true;
		}
		
		return false;
	}
	
	@Override
	synchronized public boolean writeToServer(String out)
	{
		if(isRunning())
		{
			var outStream = process.getOutputStream();
			try
			{
				outStream.write(out.getBytes());
				outStream.write(System.lineSeparator().getBytes());
				outStream.flush();
				return true;
			}
			catch(IOException e)
			{
				StartUpApplication.LOGGER.log(Level.WARNING, String.format("Failed to write to server MinecraftServer:\n%s", e.getMessage()));
			}
		}
		
		return false;
	}
	
	public String[] getRunCommand()
	{
		var command = new LinkedList<String>();
		
		command.add(NodeProperties.JAVA8);
		command.add(String.format(MAXIMUM_HEAP_ARGUMENT, maximumHeapSize));
		command.add(String.format(MINIMUM_HEAP_ARGUMENT, MINIMUM_HEAP_SIZE));
		command.addAll(Arrays.asList(arguments.split(" ")));
		command.add("-jar");
		command.add(String.format("\"%s\"", getExecutableName().getAbsolutePath()));
		command.add("nogui");
		
		var fullCommand = command.toArray(String[]::new);
		StartUpApplication.LOGGER.log(Level.INFO, String.format("Running command: %s\n", String.join(" ", fullCommand)));
		return fullCommand;
	}
	
	public BoundedCircularList<String> getLastRead()
	{
		return lastReadBounded;
	}
	
	public Properties getProperties()
	{
		var properties = new Properties();
		var propertiesFile = getFolderLocation().toPath().resolve(Path.of(SERVER_PROPERTIES_FILE_NAME)).toFile();
		if(propertiesFile.exists())
		{
			try(var s = new FileInputStream(propertiesFile))
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
		var propertiesFile = getFolderLocation().toPath().resolve(Path.of(SERVER_PROPERTIES_FILE_NAME)).toFile();
		var properties = new Properties();
		for(var prop : MINECRAFT_PROPERTIES.keySet())
		{
			properties.setProperty(prop, String.valueOf(MINECRAFT_PROPERTIES.get(prop)));
		}
		
		if(p != null)
		{
			for(var name : p.stringPropertyNames())
			{
				properties.setProperty(name, p.getProperty(name));
			}
		}
		
		try(var o = new FileOutputStream(propertiesFile))
		{
			properties.store(o, String.format("Minecraft server properties"));
		} catch (IOException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, String.format("Unable to write to MinecraftServer properties file:\n%s", e.getMessage()));
			return false;
		}
		
		return true;
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
	
	@Override
	public CommandHandler<MinecraftServer> getCommandHandler()
	{
		return new MinecraftServerCommandHandler(this);
	}
}
