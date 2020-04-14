package nodemain;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.sun.management.OperatingSystemMXBean;

import model.Database;
import model.Filter.FilterType;
import model.Query;
import model.Table;
import models.GameServerTable;
import models.NodeTable;
import models.TriggersTable;
import nodeapi.NodeUsage;
import nodeapi.Output;
import server.GameServer;
import server.GameServerFactory;
import server.GameServerModule;
import server.TriggerHandlerFactory;
import server.TriggerHandlerRecurring;
import server.TriggerHandlerTime;
import utils.TimerTaskID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class StartUpApplication implements ServletContextListener
{
	private static final Map<String, GameServerModule> loadedModules = new HashMap<String, GameServerModule>();
	private static final Map<Integer, GameServer> servers = Collections.synchronizedMap(new HashMap<Integer, GameServer>());;
	private static final Map<GameServer, Integer> serverToID = Collections.synchronizedMap(new HashMap<GameServer, Integer>());
	public static final Logger LOGGER = LoggerFactory.getLogger(StartUpApplication.class);
	public static final OperatingSystemMXBean SYSTEM = OperatingSystemMXBean.class.cast(ManagementFactory.getOperatingSystemMXBean());
	
	public static Database database;
	
	public void contextInitialized(ServletContextEvent event)
	{	
		try
		{
			Database.registerDriver("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e.getMessage());
		}
		
		database = new Database(NodeProperties.DATABASE_URL, NodeProperties.DATABASE_USERNAME, NodeProperties.DATABASE_PASSWORD);
		
		if(!database.canConnect())
		{
			throw new RuntimeException("Is the database up?");
		}
		
		GameServer.setup(database);
		
		try
		{
			var myGameServers = Query.query(database, GameServerTable.class)
									 .filter(GameServerTable.NODE_OWNER, NodeProperties.NAME)
									 .all();
			
			for(var server : myGameServers)
			{
				var specificServer = GameServerFactory.getSpecificServer(server);
				var id = server.getColumnValue(GameServerTable.ID);
				servers.put(id, specificServer);
				serverToID.put(specificServer, id);
			}
		}
		catch(SQLException e)
		{
			LOGGER.error(e.getMessage());
			return;
		}
		
		try
		{
			var triggers = Query.query(StartUpApplication.database, TriggersTable.class)
								.join(new NodeTable(), NodeTable.NAME.cloneWithValue(NodeProperties.NAME), FilterType.EQUAL)
								.join(new GameServerTable(), GameServerTable.ID, FilterType.EQUAL, new TriggersTable(), TriggersTable.SERVER_OWNER)
								.join(new GameServerTable(), GameServerTable.NODE_OWNER, FilterType.EQUAL, new NodeTable(), NodeTable.NAME)
								.all();
			
			for(var trigger : triggers)
			{
				addTrigger(trigger);
			}
			
		} catch (SQLException e)
		{
			throw new RuntimeException(e.getMessage());
		}
	}
	
	public static void addTrigger(Table trigger)
	{
		removeTrigger(trigger);
		var correspondingServer = getServer(trigger.getColumnValue(TriggersTable.SERVER_OWNER));
		if(correspondingServer != null)
		{
			var generatedHandler = TriggerHandlerFactory.getSpecificTriggerHandler(trigger, correspondingServer);
			if(generatedHandler != null)
			{
				correspondingServer.getTriggerHandlers().add(generatedHandler);
				if(generatedHandler instanceof TriggerHandlerTime)
				{
					var time = TriggerHandlerTime.class.cast(generatedHandler).getTimeExecuted();
					var generatedTask = TriggerHandlerTime.class.cast(generatedHandler).generateTimerTask();
					correspondingServer.getTimerTasks().add(generatedTask);
					
					var today = Calendar.getInstance();
					today.set(Calendar.HOUR_OF_DAY, time.getHour());
					today.set(Calendar.MINUTE, time.getMinute());
					today.set(Calendar.SECOND, time.getSecond());
					
					if(today.before(Calendar.getInstance()))
					{
						today.roll(Calendar.DATE, 1);
					}
					
					correspondingServer.getTriggerTimer().scheduleAtFixedRate(generatedTask, today.getTime(), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
				}
				else if(generatedHandler instanceof TriggerHandlerRecurring)
				{
					var secondInterval = TriggerHandlerRecurring.class.cast(generatedHandler).getRecurringPeriod();
					
					var generatedTask = TriggerHandlerRecurring.class.cast(generatedHandler).generateTimerTask();
					
					correspondingServer.getTimerTasks().add(generatedTask);
					
					correspondingServer.getTriggerTimer().scheduleAtFixedRate(generatedTask, 0, TimeUnit.MILLISECONDS.convert(secondInterval, TimeUnit.SECONDS));
				}
			}
		}
	}
	
	public static void removeTrigger(Table trigger)
	{
		var correspondingServer = getServer(trigger.getColumnValue(TriggersTable.SERVER_OWNER));
		if(correspondingServer != null)
		{
			var triggerID = trigger.getColumnValue(TriggersTable.ID);
			correspondingServer.getTriggerHandlers().removeIf(t -> t.getID() == triggerID.longValue());
			correspondingServer.getTimerTasks().removeIf(timer -> {
				if(timer instanceof TimerTaskID)
				{
					if(TimerTaskID.class.cast(timer).getID() == triggerID.longValue())
					{
						timer.cancel();
						return true;
					}
				}
				return false;
			});
		}
	}
	
	public void contextDestroyed(ServletContextEvent event)
	{
		servers.values()
			   .parallelStream()
			   .forEach(server -> {
				   try
				{
					server.stopServer();
				} catch (IOException e)
				{
					StartUpApplication.LOGGER.error(String.format("Error stopping server:\n%s", e.getMessage()));
				}
				   server.getTriggerTimer().cancel();
			   });
		
		if(!Output.OUTPUT_THREADS.isShutdown())
		{
			Output.OUTPUT_THREADS.shutdownNow();
		}
		if(!NodeUsage.TIMER.isShutdown())
		{
			NodeUsage.TIMER.shutdownNow();
		}
	}
	
	public static GameServer getServer(int id)
	{
		return servers.get(id);
	}
	
	public static Integer getID(GameServer server)
	{
		return serverToID.get(server);
	}
	
	public static Map<Integer, GameServer> getServers()
	{
		return Collections.unmodifiableMap(servers);
	}
	
	public static void addServer(int id, GameServer server)
	{
		servers.put(id, Objects.requireNonNull(server));
		serverToID.put(server, id);
	}
	
	public static void removeServer(int id)
	{
		serverToID.remove(servers.remove(id));
	}
	
	public static File getDeployFolder()
	{
		return Paths.get(NodeProperties.DEPLOY_FOLDER).toFile();
	}
	
	public static File getServerFolder(Table server)
	{
		return getDeployFolder().toPath().resolve(server.getColumnValue(GameServerTable.NAME)).toFile();
	}
	
	public static File getExecutableFile(Table server)
	{
		return getServerFolder(server).toPath().resolve(server.getColumnValue(GameServerTable.EXECUTABLE_NAME)).toFile();
	}
	
	public static GameServerModule getModule(String name)
	{
		synchronized (loadedModules)
		{
			return loadedModules.get(name);
		}
	}
	
	public static void addModule(GameServerModule module)
	{
		synchronized (loadedModules)
		{
			loadedModules.put(module.gameServerOptions().getServerType(), module);
		}
	}
}
