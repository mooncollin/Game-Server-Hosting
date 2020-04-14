package minecraft.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import minecraft.utils.BoundedCircularList;
import module.MinecraftGameServerModule;
import server.GameServer;
import server.TriggerHandlerCondition;
import server.TriggerHandlerCondition.TriggerHandlerConditionType;

public class MinecraftServer extends GameServer
{
	public static final int MINIMUM_HEAP_SIZE = 1024;
	public static final int HEAP_STEP = 1024;
	public static final int DEFAULT_LAST_READ_MAXIMUM_SIZE = 500;
	public static final String SERVER_PROPERTIES_FILE_NAME = "server.properties";
	public static final String MINIMUM_HEAP_ARGUMENT = "-Xms%dm";
	public static final String MAXIMUM_HEAP_ARGUMENT = "-Xmx%dm";
	public static final String SERVER_TYPE = "minecraft";
	
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
	
	private boolean expectedShutdown;
	private BoundedCircularList<String> lastReadBounded;
	private ExecutorService execService;
	private final ProcessBuilder processBuilder;
	private final Runnable inputTask = new Runnable()
	{
		@SuppressWarnings("unchecked")
		public void run()
		{
			try(var reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
			{
				while(isRunning() && !Thread.currentThread().isInterrupted())
				{
					String lineRead;
					try
					{
						lineRead = reader.readLine();
					} catch (IOException e1)
					{
						MinecraftGameServerModule.LOGGER.error(String.format("Reading from minecraft server caused an error:\n%s", e1.getMessage()));
						break;
					}
					
					if(lineRead == null || Thread.currentThread().isInterrupted())
					{
						break;
					}
					
					final var lineTerminated = lineRead + "\r\n";
					var triggers = getTriggerHandlers();
	
					triggers.stream()
						.filter(trigger -> trigger instanceof TriggerHandlerCondition)
						.map(trigger -> (TriggerHandlerCondition<String>) trigger)
						.filter(condition -> condition.getType().equals(TriggerHandlerConditionType.OUTPUT))
						.forEach(condition -> condition.trigger(lineRead));
						
					var streamsToRemove = new LinkedList<OutputStream>();
					
					var outputConnectors = getOutputConnectors();
					
					synchronized (outputConnectors)
					{
						for(var stream : outputConnectors)
						{
							synchronized (stream)
							{
								try
								{
									stream.write(lineTerminated.getBytes());
								} catch (IOException e)
								{
									streamsToRemove.add(stream);
								}
								stream.notify();
							}
						}
						
						outputConnectors.removeAll(streamsToRemove);
					}
					
					synchronized(lastReadBounded)
					{
						lastReadBounded.add(lineTerminated);
					}
				}
			} catch (IOException e2)
			{
				MinecraftGameServerModule.LOGGER.warn(String.format("Closing input stream to minecraft server threw an exception:\n%s", e2.getMessage()));
			}
		}
	};

	public MinecraftServer(File folderLocation, File fileName)
	{
		super(folderLocation, fileName);
		execService = Executors.newSingleThreadExecutor();
		lastReadBounded = new BoundedCircularList<String>(DEFAULT_LAST_READ_MAXIMUM_SIZE);
		
		setProperties(getProperties());
		processBuilder = new ProcessBuilder();
		processBuilder.redirectErrorStream(true);
	}
	
	@Override
	synchronized public boolean stopServer() throws IOException
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
	synchronized public boolean startServer() throws IOException
	{
		if(!isRunning())
		{
			processBuilder.directory(getFolderLocation());
			try
			{
				processBuilder.command(getRunCommand());
			} catch (SQLException e)
			{
				MinecraftGameServerModule.LOGGER.error(String.format("Unable to get server options from the database:\n%s", e.getMessage()));
			}
			
			process = processBuilder.start();
			notifyRunningNotifiers();
			expectedShutdown = false;
			process.onExit().thenAcceptAsync(p -> {
				notifyRunningNotifiers();
				try
				{
					if(!expectedShutdown && getGameServerOptions().autoRestarts())
					{
						try
						{
							Thread.sleep(5000);
							startServer();
						}
						catch (InterruptedException e)
						{
						}
						catch(IOException e)
						{
							MinecraftGameServerModule.LOGGER.error(String.format("Error auto-restarting server:\n%s", e.getMessage()));
						}
					}
				} catch (SQLException e)
				{
					MinecraftGameServerModule.LOGGER.error(String.format("Error getting auto-restart value from database:\n%s", e.getMessage()));
				}
			});
			
			lastReadBounded.clear();
			execService.submit(inputTask);
			return true;
		}
		
		return false;
	}
	
	@Override
	synchronized public boolean writeToServer(Reader in) throws IOException
	{
		if(isRunning())
		{
			var outStream = new OutputStreamWriter(process.getOutputStream());
			in.transferTo(outStream);
			outStream.write(System.lineSeparator());
			outStream.flush();
			return true;
		}
		
		return false;
	}
	
	@Override
	synchronized public boolean readFromServer(Writer out) throws IOException
	{
		if(isRunning())
		{
			try(var inStream = new InputStreamReader(process.getInputStream()))
			{
				inStream.transferTo(out);
			}
			return true;
		}
		return false;
	}
	
	public String[] getRunCommand() throws SQLException
	{
		var command = new LinkedList<String>();
		
		command.add(MinecraftGameServerModule.JAVA8);
		command.add(String.format(MAXIMUM_HEAP_ARGUMENT, getGameServerOptions().maxRam()));
		command.add(String.format(MINIMUM_HEAP_ARGUMENT, MINIMUM_HEAP_SIZE));
		for(var argument : getGameServerOptions().arguments().split(" "))
		{
			if(!argument.isBlank())
			{
				command.add(argument);
			}
		}
		command.add("-jar");
		command.add(getExecutableFile().getAbsolutePath());
		command.add("nogui");
		
		var fullCommand = command.toArray(String[]::new);
		MinecraftGameServerModule.LOGGER.info(String.format("Running command: %s\n", String.join(" ", fullCommand)));
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
				MinecraftGameServerModule.LOGGER.error(String.format("Unable to read MinecraftServer properties file:\n%s", e.getMessage()));
				return null;
			}
		}
		else
		{
			MinecraftGameServerModule.LOGGER.warn("Minecraft Server properties file is missing. Might be first time loading.");
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
			MinecraftGameServerModule.LOGGER.error(String.format("Unable to write to MinecraftServer properties file:\n%s", e.getMessage()));
			return false;
		}
		
		return true;
	}
	
	public String getServerType()
	{
		return SERVER_TYPE;
	}
	
	@Override
	public String getPublicIPAddress()
	{
		var ip = super.getPublicIPAddress();
		if(ip == null)
		{
			return null;
		}
		
		var properties = getProperties();
		return String.format("%s:%s", ip, properties.get("server-port"));
	}
	
	@Override
	public MinecraftServerCommandHandler getCommandHandler()
	{
		return getGameServerOptions().getCommandHandler();
	}
	
	@Override
	public MinecraftServerOptions getGameServerOptions()
	{
		return new MinecraftServerOptions(this);
	}
	
	@Override
	public String getLog()
	{
		synchronized(lastReadBounded)
		{
			return String.join("", List.of(lastReadBounded.toArray(String[]::new)));
		}
	}
}
