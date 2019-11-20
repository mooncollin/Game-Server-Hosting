package backend.main;

import java.sql.SQLException;
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
import utils.Pair;

@WebListener
public class StartUpApplication implements ServletContextListener
{
	
	private static final Map<String, Pair<Class<? extends GameServer>, String>> serverNamesToAddresses = Collections.synchronizedMap(new HashMap<String, Pair<Class<? extends GameServer>, String>>());
	
	public static final String NODE_OUTPUT_URL = "/Output";
	public static String[] NODE_NAMES;
	public static String[] NODE_ADDRESSES;
	public static String[] NODE_PORTS;
	public static final Logger LOGGER = Logger.getGlobal();
	public static Database database;
	
	public static Map<String, Pair<Class<? extends GameServer>, String>> getServerInfo()
	{
		return serverNamesToAddresses;
	}

	public void contextInitialized(ServletContextEvent sce)
	{
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
		NODE_PORTS = ControllerProperties.NODE_PORTS.split(",");
		var extension = ControllerProperties.NODE_EXTENSION;
		
		for(var i = 0; i < NODE_NAMES.length; i++)
		{
			var url = createNodeURL(NODE_ADDRESSES[i], NODE_PORTS[i], extension);
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
				serverNamesToAddresses.put(
					server.getColumn(GameServerTable.NAME).getValue(),
					Pair.of(GameServer.PROPERTY_NAMES_TO_TYPE.get(server.getColumn(GameServerTable.SERVER_TYPE).getValue()), url));
			}
		}
	}
	
	public static String createNodeURL(String address, String port, String extension)
	{
		return String.format("%s:%s/%s", address, port, extension);
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

	public void contextDestroyed(ServletContextEvent sce)
	{
	}
}
