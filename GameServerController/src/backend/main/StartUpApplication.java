package backend.main;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import api.minecraft.MinecraftServer;
import frontend.Endpoints;
import model.Database;
import model.Query;
import model.Filter.FilterType;
import models.GameServerTable;
import models.MinecraftServerTable;
import server.GameServer;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.FieldMethodizer;
import org.apache.velocity.app.Velocity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@WebListener
public class StartUpApplication implements ServletContextListener
{
	private static final Map<Integer, String> serverIPAddresses = new HashMap<Integer, String>();
	private static final Map<String, String> nodeIPAddresses = new HashMap<String, String>();
	
	public static final List<String> NODE_NAMES = Arrays.asList(ControllerProperties.NODE_NAMES.split(","));
	public static final List<String> NODE_ADDRESSES = Arrays.asList(ControllerProperties.NODE_ADDRESSES.split(","));;
	public static final Logger LOGGER = LoggerFactory.getLogger(StartUpApplication.class);
	public static Database database;
	
	public static final String SERVLET_PATH = "/GameServerController";
	public static final VelocityContext GLOBAL_CONTEXT = new VelocityContext();
	public static HttpClient client = HttpClient.newBuilder()
											    .version(Version.HTTP_1_1)
											    .followRedirects(Redirect.NORMAL)
											    .connectTimeout(Duration.ofSeconds(20))
											    .build();

	public void contextInitialized(ServletContextEvent sce)
	{
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
		MinecraftServer.setup(database);
		
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
			throw new RuntimeException(String.format("Error when starting up controller: %s", e.getMessage()));
		}
	}
	
	public static long getNodeReservedRam(String nodeName) throws SQLException
	{
		var minecraftServers = Query.query(database, MinecraftServerTable.class)
									.join(new GameServerTable(), GameServerTable.ID, FilterType.EQUAL, new MinecraftServerTable(), MinecraftServerTable.ID)
									.filter(GameServerTable.NODE_OWNER, nodeName)
									.all();
		
		return minecraftServers.parallelStream()
							   .mapToLong(server -> server.getColumnValue(MinecraftServerTable.MAX_HEAP_SIZE).longValue())
							   .sum();
	}
	
	public static String getNodeIPAddress(String nodeName)
	{
		return nodeIPAddresses.get(nodeName);
	}
	
	public static String getServerIPAddress(int serverID)
	{
		synchronized (serverIPAddresses)
		{
			return serverIPAddresses.get(serverID);
		}
	}
	
	public static void addServerIPAddress(int serverID, String nodeName)
	{
		synchronized (serverIPAddresses)
		{
			serverIPAddresses.put(serverID, getNodeIPAddress(nodeName));
		}
	}
	
	public static void removeServerIPAddress(int serverID)
	{
		synchronized (serverIPAddresses)
		{
			serverIPAddresses.remove(serverID);
		}
	}
	
	public static Map<Integer, String> getStartServerAddresses()
	{
		synchronized(serverIPAddresses)
		{
			return StartUpApplication.serverIPAddresses.keySet()
					.parallelStream()
					.collect(Collectors.toMap(Function.identity(),
											  k -> Endpoints.SERVER_INTERACT.get(k, "start").getURL()));
		}
	}
	
	public static Map<Integer, String> getStopServerAddresses()
	{
		synchronized(serverIPAddresses)
		{
			return StartUpApplication.serverIPAddresses.keySet()
					.parallelStream()
					.collect(Collectors.toMap(Function.identity(),
											  k -> Endpoints.SERVER_INTERACT.get(k, "stop").getURL()));
		}
	}
	
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

	public void contextDestroyed(ServletContextEvent sce)
	{
		client = null;
	}
}
