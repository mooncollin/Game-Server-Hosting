package main;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import api.NodeUsage;
import api.Output;
import api.minecraft.MinecraftServer;
import model.Database;
import model.Filter.FilterType;
import model.Query;
import model.Table;
import models.GameServerTable;
import models.NodeTable;
import models.TriggersTable;
import server.GameServer;
import server.GameServerFactory;
import server.TriggerHandlerFactory;
import server.TriggerHandlerRecurring;
import server.TriggerHandlerTime;
import utils.Pair;
import utils.BoundedCircularList;
import utils.TimerTaskID;

@WebListener
public class StartUpApplication implements ServletContextListener
{
	
	private static final Map<Integer, GameServer> servers = Collections.synchronizedMap(new HashMap<Integer, GameServer>());;
	private static final Map<GameServer, Integer> serverToID = Collections.synchronizedMap(new HashMap<GameServer, Integer>());
	public static final BoundedCircularList<Pair<Integer, Long>> nodeUsage = new BoundedCircularList<Pair<Integer, Long>>(500);
	public static final long NODE_USAGE_WAIT_TIME = 900;
	public static final Logger LOGGER = Logger.getGlobal();
	
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
		MinecraftServer.setup(database);
		
		try
		{
			var option = Query.query(StartUpApplication.database, NodeTable.class)
						  .filter(NodeTable.NAME, NodeProperties.NAME)
						  .first();
			
			Table me;
			
			if(option.isEmpty())
			{
				me = new NodeTable();
				me.setColumnValue(NodeTable.NAME, NodeProperties.NAME);
				me.setColumnValue(NodeTable.MAX_RAM_ALLOWED, NodeProperties.MAX_RAM);
			}
			else
			{
				me = option.get();
				me.setColumnValue(NodeTable.MAX_RAM_ALLOWED, NodeProperties.MAX_RAM);
			}
			
			me.commit(StartUpApplication.database);
		}
		catch(SQLException e)
		{
			throw new RuntimeException("Error starting up node: " + e.getMessage());
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
			LOGGER.log(Level.SEVERE, e.getMessage());
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
					var time = ((TriggerHandlerTime) generatedHandler).getTimeExecuted();
					var generatedTask = ((TriggerHandlerTime) generatedHandler).generateTimerTask();
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
					var secondInterval = ((TriggerHandlerRecurring) generatedHandler).getRecurringPeriod();
					
					var generatedTask = ((TriggerHandlerRecurring) generatedHandler).generateTimerTask();
					
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
					if(((TimerTaskID) timer).getID() == triggerID.longValue())
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
		for(GameServer server : servers.values())
		{
			server.stopServer();
			server.getTriggerTimer().cancel();
		}
		
		if(Output.execService != null)
		{
			Output.execService.shutdownNow();
		}
		if(NodeUsage.execService != null)
		{
			NodeUsage.execService.shutdownNow();
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
}
