package backend.main;


import java.io.File;
import java.io.FileOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import frontend.Endpoints;
import model.Database;
import model.Query;
import models.GameModuleTable;
import models.GameServerTable;
import server.GameServer;
import server.GameServerModule;
import server.GameServerModuleLoader;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.FieldMethodizer;
import org.apache.velocity.app.Velocity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@WebListener
/**
 * This class holds instance data for the currently running controller.
 * @author Collin
 *
 */
public class StartUpApplication implements ServletContextListener
{
	/**
	 * A mapping from server IDs to their corresponding node's ip address.
	 */
	private static final Map<Integer, String> serverIPAddresses = new HashMap<Integer, String>();
	
	/**
	 * A mapping from a node's name to its corresponding ip address.
	 */
	private static final Map<String, String> nodeIPAddresses = new HashMap<String, String>();
	
	/**
	 * The executor used in the global http client.
	 */
	private static final ExecutorService HTTP_CLIENT_EXECUTOR = Executors.newWorkStealingPool();
	
	/**
	 * A mapping from a module's name to its corresponding runtime implementations.
	 */
	private static final Map<String, GameServerModule> loadedModules = new HashMap<String, GameServerModule>();
	
	/**
	 * A proper list of current node names.
	 */
	public static final List<String> NODE_NAMES = Arrays.asList(ControllerProperties.NODE_NAMES.split(","));
	
	/**
	 * A proper list of current node addresses.
	 */
	public static final List<String> NODE_ADDRESSES = Arrays.asList(ControllerProperties.NODE_ADDRESSES.split(","));;
	
	/**
	 * The global logger.
	 */
	public static final Logger LOGGER = LoggerFactory.getLogger(StartUpApplication.class);
	
	/**
	 * The global database communication object.
	 */
	public static Database database;
	
	/**
	 * The servlet path of this application.
	 */
	public static final String SERVLET_PATH = "/GameServerController";
	
	/**
	 * The full path to the current application's root directory.
	 */
	public static String APPLICATION_ROOT;
	
	/**
	 * The global context for Apache Velocity.
	 */
	public static final VelocityContext GLOBAL_CONTEXT = new VelocityContext();
	
	/**
	 * The global http client to use to send http requests.
	 */
	public static HttpClient client = HttpClient.newBuilder()
											    .version(Version.HTTP_1_1)
											    .followRedirects(Redirect.NORMAL)
											    .executor(HTTP_CLIENT_EXECUTOR)
											    .connectTimeout(Duration.ofSeconds(20))
											    .build();

	/**
	 * Initializes the game server controller's runtime startup variables.
	 * @param sce the context of the servlets
	 */
	public void contextInitialized(ServletContextEvent sce)
	{
		APPLICATION_ROOT = sce.getServletContext().getRealPath("/");
		Velocity.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, sce.getServletContext().getRealPath(ControllerProperties.TEMPLATES_PATH));
		Velocity.init();
		GLOBAL_CONTEXT.put("Endpoints", new FieldMethodizer(Endpoints.class.getName()));
		
		try
		{
			Database.registerDriver("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e.getMessage());
		}
		
		database = new Database(ControllerProperties.DATABASE_URL, ControllerProperties.DATABASE_USERNAME, ControllerProperties.DATABASE_PASSWORD);
		
		if(!database.canConnect())
		{
			throw new RuntimeException("Is the database up?");
		}
		
		GameServer.setup(database);	
		
		var nodeNameIt = NODE_NAMES.iterator();
		var nodeAddrIt = NODE_ADDRESSES.iterator();
		
		while(nodeNameIt.hasNext() && nodeAddrIt.hasNext())
		{
			var nodeName = nodeNameIt.next();
			var nodeAddr = nodeAddrIt.next();
			nodeIPAddresses.put(nodeName, nodeAddr);
		}

		try
		{
			var gameServers = Query.query(StartUpApplication.database, GameServerTable.class)
								   .all();
			
			for(var server : gameServers)
			{
				var id = server.getColumnValue(GameServerTable.ID);
				var nodeOwner = server.getColumnValue(GameServerTable.NODE_OWNER);
				addServerIPAddress(id, nodeOwner);
			}
		} catch (SQLException e)
		{
			throw new RuntimeException(String.format("Error when starting up controller:\n%s", e.getMessage()));
		}
		
		try
		{
			var modules = Query.query(StartUpApplication.database, GameModuleTable.class)
							   .all();
			
			for(var module : modules)
			{
				var tempFile = Files.createTempFile("gametype", ".jar");
				try
				{
					var jarData = (byte[]) module.getColumnValue(GameModuleTable.JAR.getName());
					try(var fileOut = new FileOutputStream(tempFile.toFile()))
					{
						fileOut.write(jarData);
					}
					
					var instantiatedModule = GameServerModuleLoader.loadModule(tempFile.toFile());
					if(instantiatedModule != null)
					{
						StartUpApplication.addModule(instantiatedModule);
						GameServerModuleLoader.loadModuleResources(tempFile.toFile(), new File(APPLICATION_ROOT));
					}
				}
				finally
				{
					tempFile.toFile().delete();
				}
			}
		}
		catch(Exception e)
		{
			throw new RuntimeException(String.format("Error when starting up controller:\n%s", e.getMessage()));
		}
	}
	
	/**
	 * Gets a node's ip address from its name.
	 * @param nodeName the name of a node
	 * @return the corresponding ip address
	 */
	public static String getNodeIPAddress(String nodeName)
	{
		return nodeIPAddresses.get(nodeName);
	}
	
	/**
	 * Gets all the nodes' ip addresses.
	 * @return a list of ip addresses
	 */
	public static Collection<String> getNodeIPAddresses()
	{
		return nodeIPAddresses.values();
	}
	
	/**
	 * Gets a node's ip address of the server that has the given id.
	 * @param serverID an id from the database.
	 * @return a node's ip address
	 */
	public static String getServerIPAddress(int serverID)
	{
		synchronized (serverIPAddresses)
		{
			return serverIPAddresses.get(serverID);
		}
	}
	
	/**
	 * Adds a mapping between a server id from the database and the ip
	 * address associated with the node name.
	 * @param serverID id from the database
	 * @param nodeName name of a node
	 */
	public static void addServerIPAddress(int serverID, String nodeName)
	{
		synchronized (serverIPAddresses)
		{
			serverIPAddresses.put(serverID, getNodeIPAddress(nodeName));
		}
	}
	
	/**
	 * Removes a server ip address mapping.
	 * @param serverID id from the database
	 */
	public static void removeServerIPAddress(int serverID)
	{
		synchronized (serverIPAddresses)
		{
			serverIPAddresses.remove(serverID);
		}
	}

	/**
	 * Creates and gathers a mapping of server ids to their corresponding
	 * nodes' output web socket endpoint with the given mode selected.
	 * @param mode a string indicating the mode of output, either 'running' or 'output'
	 * to signal only wanting to listen to those kinds of messages, or anything else
	 * to signal wanting to listen to both
	 * @return a server id to url mapping
	 */
	public static Map<Integer, String> getNodeOutputAddresses(String mode)
	{
		synchronized(serverIPAddresses)
		{
			return StartUpApplication.serverIPAddresses.keySet()
					.parallelStream()
					.collect(Collectors.toMap(Function.identity(), k -> {
						var nodeAddress = StartUpApplication.serverIPAddresses.get(k);
						var url = nodeapi.Output.getEndpoint(k, mode);
						url.setHost(nodeAddress);
						return url.getURL();
					}));
		}
	}
	
	/**
	 * Adds a module to the list of currently loaded modules.
	 * @param module an implementation of a custom game server
	 */
	public static void addModule(GameServerModule module)
	{
		synchronized (loadedModules)
		{
			loadedModules.put(module.gameServerOptions().getServerType(), module);
		}
	}
	
	/**
	 * Gets a set of the currently loaded server types.
	 * @return current server types
	 */
	public static Set<String> getServerTypes()
	{
		synchronized (loadedModules)
		{
			return Collections.unmodifiableSet(loadedModules.keySet());
		}
	}
	
	/**
	 * Returns a module implementation based on its type.
	 * @param name the name of the module
	 * @return a module implementation associated with the given name
	 */
	public static GameServerModule getModule(String name)
	{
		synchronized (loadedModules)
		{
			return loadedModules.get(name);
		}
	}

	/**
	 * Function called when this application shuts down.
	 * @param sce a servlet context 
	 */
	@Override
	public void contextDestroyed(ServletContextEvent sce)
	{
		HTTP_CLIENT_EXECUTOR.shutdownNow();
		try
		{
			if(!HTTP_CLIENT_EXECUTOR.awaitTermination(3, TimeUnit.SECONDS))
			{
				throw new RuntimeException("Unable to shutdown http client");
			}
		} catch (InterruptedException e)
		{
		}
	}
}
