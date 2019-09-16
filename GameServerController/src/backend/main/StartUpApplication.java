package backend.main;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import model.Model;
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
	
	public static Map<String, Pair<Class<? extends GameServer>, String>> getServerInfo()
	{
		return serverNamesToAddresses;
	}

	public void contextInitialized(ServletContextEvent sce)
	{
		Model.registerDriver();
		Model.setURL(ControllerProperties.DATABASE_URL);
		Model.setUsername(ControllerProperties.DATABASE_USERNAME);
		Model.setPassword(ControllerProperties.DATABASE_PASSWORD);
		
		Connection connection = Model.getConnection();
		if(connection == null)
		{
			throw new RuntimeException("Is the database up?");
		}
		try
		{
			connection.close();
		} catch (SQLException e)
		{
		}
		
		NODE_NAMES = ControllerProperties.NODE_NAMES.split(",");
		NODE_ADDRESSES = ControllerProperties.NODE_ADDRESSES.split(",");
		NODE_PORTS = ControllerProperties.NODE_PORTS.split(",");
		String extension = ControllerProperties.NODE_EXTENSION;
		
		for(int i = 0; i < NODE_NAMES.length; i++)
		{
			String url = createNodeURL(NODE_ADDRESSES[i], NODE_PORTS[i], extension);
			List<models.GameServer> gameServers = Model.getAll(models.GameServer.class, "nodeOwner=?", NODE_NAMES[i]);
			for(models.GameServer server : gameServers)
			{
				serverNamesToAddresses.put(server.getName(), new Pair<Class<? extends GameServer>, String>(GameServer.PROPERTY_NAMES_TO_TYPE.get(server.getServerType()), url));
			}
		}
	}
	
	public static String createNodeURL(String address, String port, String extension)
	{
		return String.format("%s:%s/%s", address, port, extension);
	}
	
	public static long getNodeReservedRam(String nodeName)
	{
		long reservedRam = 0;
		List<models.GameServer> thisNodesServers = Model.getAll(models.GameServer.class, "nodeOwner=?", nodeName);
		List<Integer> minecraftIDs = new LinkedList<Integer>();
		for(models.GameServer gameServer : thisNodesServers)
		{
			if(gameServer.getServerType().equals("minecraft"))
			{
				minecraftIDs.add(gameServer.getSpecificID());
			}
		}
		String queryString = minecraftIDs.stream()
				 .mapToInt(id -> id)
				 .mapToObj(id -> "id=?")
				 .reduce((first, second) -> first + " or " + second).orElseGet(() -> null);
		if(queryString != null)
		{
			List<models.MinecraftServer> minecrafts = Model.getAll(models.MinecraftServer.class, queryString, minecraftIDs.toArray());
			for(models.MinecraftServer minecraft : minecrafts)
			{
				reservedRam += minecraft.getMaxHeapSize();
			}
		}
		
		return reservedRam;
	}

	public void contextDestroyed(ServletContextEvent sce)
	{
	}
}
