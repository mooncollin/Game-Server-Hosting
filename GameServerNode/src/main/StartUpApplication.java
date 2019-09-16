package main;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import api.NodeUsage;
import api.minecraft.Output;
import model.Model;
import server.GameServer;
import server.GameServerFactory;
import server.TriggerHandler;
import server.TriggerHandlerFactory;
import server.TriggerHandlerRecurring;
import server.TriggerHandlerTime;
import utils.BoundedCircularList;
import utils.Pair;
import utils.TimerTaskID;

@WebListener
public class StartUpApplication implements ServletContextListener
{
	
	private static final List<GameServer> servers;
	public static final String NODE_NAME;
	public static final String JAVA8;
	public static final BoundedCircularList<Pair<Integer, Long>> nodeUsage = new BoundedCircularList<Pair<Integer, Long>>(500);
	public static final long NODE_USAGE_WAIT_TIME = 900;
	public static final Logger LOGGER = Logger.getGlobal();
	
	static
	{
		servers = Collections.synchronizedList(new LinkedList<GameServer>());
		Properties properties = NodeProperties.getProperties();
		NODE_NAME = properties.getProperty("name");
		
		JAVA8 = Objects.requireNonNull(System.getenv("JAVA8"), "Do you have the JAVA8 environment variable set?");
	}
	
	public void contextInitialized(ServletContextEvent event)
	{
		Properties properties = NodeProperties.getProperties();
		
		Model.registerDriver();
		Model.setURL(properties.getProperty("database_url"));
		Model.setUsername(properties.getProperty("database_username"));
		Model.setPassword(properties.getProperty("database_password"));
		
		List<models.GameServer> myGameServers = Objects.requireNonNull(Model.getAll(models.GameServer.class, "nodeOwner=?", NODE_NAME),
				"Is the database up?");
		
		for(var server : myGameServers)
		{
			servers.add(GameServerFactory.getSpecificServer(server));
		}
		
		List<models.Node> meList = Model.getAll(models.Node.class, "name=?", NodeProperties.NAME);
		models.Node me;
		if(meList.isEmpty())
		{
			models.Node newNode = new models.Node(NODE_NAME, NodeProperties.MAX_RAM);
			me = newNode;
		}
		else
		{
			me = meList.get(0);
			me.setRAM(NodeProperties.MAX_RAM);
		}
		
		if(me.changed() && !me.commit())
		{
			throw new RuntimeException("Cannot update node in the database!");
		}
		
		List<models.Triggers> triggers = Model.getAll(models.Triggers.class);
		for(models.Triggers trigger : triggers)
		{
			addTrigger(trigger);
		}
		
//		execService.execute(new NodeUsageRunnable());
	}
	
	public static void addTrigger(models.Triggers trigger)
	{
		removeTrigger(trigger);
		GameServer correspondingServer = getServer(trigger.getServerOwner());
		if(correspondingServer != null)
		{
			TriggerHandler generatedHandler = TriggerHandlerFactory.getSpecificTriggerHandler(trigger, correspondingServer);
			if(generatedHandler != null)
			{
				correspondingServer.getTriggerHandlers().add(generatedHandler);
				if(generatedHandler instanceof TriggerHandlerTime)
				{
					LocalTime time = ((TriggerHandlerTime) generatedHandler).getTimeExecuted();
					TimerTask generatedTask = ((TriggerHandlerTime) generatedHandler).generateTimerTask();
					correspondingServer.getTimerTasks().add(generatedTask);
					
					Calendar today = Calendar.getInstance();
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
					int secondInterval = ((TriggerHandlerRecurring) generatedHandler).getRecurringPeriod();
					
					TimerTask generatedTask = ((TriggerHandlerRecurring) generatedHandler).generateTimerTask();
					
					correspondingServer.getTimerTasks().add(generatedTask);
					
					correspondingServer.getTriggerTimer().scheduleAtFixedRate(generatedTask, 0, TimeUnit.MILLISECONDS.convert(secondInterval, TimeUnit.SECONDS));
				}
			}
		}
	}
	
	public static void removeTrigger(models.Triggers trigger)
	{
		GameServer correspondingServer = getServer(trigger.getServerOwner());
		if(correspondingServer != null)
		{
			correspondingServer.getTriggerHandlers().removeIf(t -> t.getID() == trigger.getID().longValue());
			correspondingServer.getTimerTasks().removeIf(timer -> {
				if(timer instanceof TimerTaskID)
				{
					if(((TimerTaskID) timer).getID() == trigger.getID().longValue())
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
		for(GameServer server : servers)
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
//		if(StartUpApplication.execService != null)
//		{
//			StartUpApplication.execService.shutdownNow();
//		}
	}
	
	public static GameServer getServer(String name)
	{
		synchronized(servers)
		{
			for(GameServer server : servers)
			{
				if(server.getName().equals(name))
				{
					return server;
				}
			}
		}
			
		return null;
	}
	
	public static List<GameServer> getServers()
	{
		return Collections.unmodifiableList(servers);
	}
	
	public static void addServer(GameServer server)
	{
		if(server != null)
		{
			servers.add(server);
		}
	}
	
	public static void removeServer(GameServer server)
	{
		servers.remove(server);
	}
}
