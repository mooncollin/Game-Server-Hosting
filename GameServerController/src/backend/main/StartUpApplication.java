package backend.main;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import api.minecraft.MinecraftServer;
import model.Database;
import model.Query;
import model.Table;
import model.Filter.FilterType;
import model.Filter.RelationType;
import models.GameServerTable;
import models.MinecraftServerTable;
import server.GameServer;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;


@WebListener
public class StartUpApplication implements ServletContextListener
{
	public static final Map<Integer, Class<? extends GameServer>> serverTypes = Collections.synchronizedMap(new HashMap<Integer, Class<? extends GameServer>>());
	public static final Map<Class<? extends GameServer>, String> serverTypesToNames = Collections.synchronizedMap(new HashMap<Class<? extends GameServer>, String>());
	public static final Map<Integer, String> serverIPAddresses = Collections.synchronizedMap(new HashMap<Integer, String>());
	public static final Map<String, String> nodeIPAddresses = new HashMap<String, String>();
	public static final Map<String, String> URL_MAPPINGS = new HashMap<String, String>();
	public static String[] NODE_NAMES;
	public static String[] NODE_ADDRESSES;
	public static final Logger LOGGER = Logger.getGlobal();
	public static Database database;
	
	public static String SERVLET_PATH;
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
		
		SERVLET_PATH = sce.getServletContext().getContextPath();
		var servlets = sce.getServletContext().getServletRegistrations();
		for(var name : servlets.keySet())
		{
			var mappings = servlets.get(name).getMappings();
			if(!mappings.isEmpty())
			{
				var mapping = servlets.get(name).getMappings().iterator().next();
				URL_MAPPINGS.put(name, SERVLET_PATH + mapping);
			}
		}
		
		GLOBAL_CONTEXT.put("urlMappings", URL_MAPPINGS);
		serverTypesToNames.put(MinecraftServer.class, "Minecraft");
		
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
		
		NODE_NAMES = ControllerProperties.NODE_NAMES.split(",");
		NODE_ADDRESSES = ControllerProperties.NODE_ADDRESSES.split(",");
		
		for(var i = 0; i < NODE_NAMES.length; i++)
		{
			nodeIPAddresses.put(NODE_NAMES[i], NODE_ADDRESSES[i]);
			List<Table> gameServers;
			try
			{
				gameServers = new GameServerTable().query(StartUpApplication.database)
													  .filter(GameServerTable.NODE_OWNER.cloneWithValue(NODE_NAMES[i]))
													  .all();
			} catch (SQLException e)
			{
				throw new RuntimeException(String.format("Error when starting up controller: %s", e.getMessage()));
			}
			
			for(var server : gameServers)
			{
				var id = server.getColumnValue(GameServerTable.ID);
				serverTypes.put(id, GameServer.PROPERTY_NAMES_TO_TYPE.get(server.getColumnValue(GameServerTable.SERVER_TYPE)));
				serverIPAddresses.put(id, NODE_ADDRESSES[i]);
			}
		}
	}
	
	public static long getNodeReservedRam(String nodeName) throws SQLException
	{
		List<Integer> minecraftIDs;
		try(var connection = database.getConnection())
		{	
			minecraftIDs = Query.query(database, MinecraftServerTable.class)
								.join(new GameServerTable(), GameServerTable.ID, FilterType.EQUAL, new MinecraftServerTable(), MinecraftServerTable.ID)
								.all()
								.stream()
								.map(server -> server.getColumnValue(MinecraftServerTable.ID))
								.collect(Collectors.toList());
		}
		
		var minecraftServersQuery = Query.query(StartUpApplication.database, MinecraftServerTable.class);
		minecraftServersQuery.filter(RelationType.OR, 
				minecraftIDs.stream().map(id -> MinecraftServerTable.ID.cloneWithValue(id)).collect(Collectors.toList()), 
				minecraftIDs.stream().map(id -> FilterType.EQUAL).collect(Collectors.toList()));
		
		var minecraftServers = minecraftServersQuery.all();
		
		return minecraftServers.stream()
							   .mapToLong(server -> server.getColumnValue(MinecraftServerTable.MAX_HEAP_SIZE).longValue())
							   .sum();
	}
	
	public static String getUrlMapping(String name)
	{
		return URL_MAPPINGS.get(name);
	}
	
	public static <T> String getUrlMapping(Class<T> clazz)
	{
		return URL_MAPPINGS.get(clazz.getName());
	}

	public void contextDestroyed(ServletContextEvent sce)
	{
		client = null;
	}
}
