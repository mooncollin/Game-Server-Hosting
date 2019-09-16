package api;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.sun.management.OperatingSystemMXBean;

import main.StartUpApplication;
import utils.Pair;

@ServerEndpoint("/NodeUsage")
public class NodeUsage
{
	public static final ExecutorService execService = Executors.newFixedThreadPool(40);
	private static final List<Pair<Session, Future<?>>> currentRunning = Collections.synchronizedList(new LinkedList<Pair<Session, Future<?>>>());
	
	@OnOpen
	public void onOpen(Session session)
	{
		Future<?> future = execService.submit(new NodeUsageRunnable(session));
		currentRunning.add(Pair.of(session, future));
	}
	
	@OnClose
	public void onClose(Session session)
	{
		try
		{
			currentRunning.removeIf(pair -> {
				Session currentSession = pair.getFirst();
				Future<?> currentRunnable = pair.getSecond();
				if(currentSession.equals(session))
				{
					currentRunnable.cancel(true);
					return true;
				}
				return false;
			});
			session.close();
		} catch (IOException e)
		{
		}
	}
	
	@OnError
	public void onError(Session session, Throwable t)
	{
		try
		{
			currentRunning.removeIf(pair -> {
				Session currentSession = pair.getFirst();
				Future<?> currentRunnable = pair.getSecond();
				if(currentSession.equals(session))
				{
					currentRunnable.cancel(true);
					return true;
				}
				return false;
			});
			session.close();
		} catch (IOException e)
		{
		}
	}
	
	private class NodeUsageRunnable implements Runnable
	{
		private Session session;
		private OperatingSystemMXBean system;
		
		public NodeUsageRunnable(Session s)
		{
			this.session = s;
			system = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		}
		
		public void run()
		{
			while(session.isOpen() && !Thread.interrupted())
			{
				int cpuLoad = (int) (system.getSystemCpuLoad() * 100);
				long totalMemory = system.getTotalPhysicalMemorySize();
				long freeMemory = system.getFreePhysicalMemorySize();
				long ramUsage = (long) (((totalMemory - freeMemory) / (double) totalMemory) * 100);
				if(cpuLoad != -1)
				{
					try
					{
						sendUsage(session, cpuLoad, ramUsage);
					} catch (IOException | IllegalStateException e)
					{
						break;
					}
				}
				
				try
				{
					Thread.sleep(StartUpApplication.NODE_USAGE_WAIT_TIME);
				} catch (InterruptedException e)
				{
					break;
				}
			}
		}
	}
	
	private void sendUsage(Session session, int cpu, long ram) throws IOException
	{
		session.getBasicRemote().sendText(String.format("%d %d", cpu, ram));
	}
}
