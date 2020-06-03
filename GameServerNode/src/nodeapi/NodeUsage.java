package nodeapi;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import nodemain.StartUpApplication;

@ServerEndpoint("/NodeUsage")
public class NodeUsage
{
	public static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor();
	public static Future<?> NODE_USAGE_TASK;
	
	private static final Duration NODE_USAGE_WAIT_TIME = Duration.ofMillis(350);
	
	private static final Set<Session> allSessions = new HashSet<Session>();
	
	@OnOpen
	public void onOpen(Session session)
	{
		synchronized (allSessions)
		{
			allSessions.add(session);
			if(allSessions.size() == 1)
			{
				NODE_USAGE_TASK = TIMER.scheduleAtFixedRate(NodeUsage::sendUsage, 0, NODE_USAGE_WAIT_TIME.toMillisPart(), TimeUnit.MILLISECONDS);
			}
		}
	}
	
	@OnClose
	public void onClose(Session session)
	{
		synchronized (allSessions)
		{
			allSessions.remove(session);
			if(allSessions.isEmpty())
			{
				NODE_USAGE_TASK.cancel(false);
			}
		}
	}
	
	private static void sendUsage()
	{
		var cpuLoad = (int) (StartUpApplication.SYSTEM.getSystemCpuLoad() * 100);
		var totalMemory = StartUpApplication.SYSTEM.getTotalPhysicalMemorySize();
		var freeMemory = StartUpApplication.SYSTEM.getFreePhysicalMemorySize();
		var ramUsage = (long) ((totalMemory - freeMemory) / ((double) totalMemory) * 100.0);
		
		if(cpuLoad >= 0)
		{
			synchronized (allSessions)
			{
				allSessions.parallelStream()
						   .forEach(session -> sendUsage(session, cpuLoad, ramUsage));
			}
		}
	}
	
	private static void sendUsage(Session session, int cpu, long ram)
	{
		session.getAsyncRemote().sendText(String.format("%d %d", cpu, ram));
	}
}
