package nodeapi;

import java.io.IOException;
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

import nodemain.StartUpApplication;
import utils.Pair;
import utils.ParameterURL;

@ServerEndpoint("/NodeUsage")
public class NodeUsage
{
	public static final String URL = "/GameServerNode/NodeUsage";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		ParameterURL.WEB_SOCKET_PROTOCOL, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL getEndpoint()
	{
		var url = new ParameterURL(PARAMETER_URL);
		return url;
	}
	
	public static final ExecutorService execService = Executors.newFixedThreadPool(40);
	private static final List<Pair<Session, Future<?>>> currentRunning = new LinkedList<Pair<Session, Future<?>>>();
	private static final long NODE_USAGE_WAIT_TIME = 900;
	
	@OnOpen
	public void onOpen(Session session)
	{
		var future = execService.submit(new NodeUsageRunnable(session));
		synchronized(currentRunning)
		{
			currentRunning.add(Pair.of(session, future));
		}
	}
	
	@OnClose
	public void onClose(Session session)
	{
		try
		{
			synchronized (currentRunning)
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
			}
			session.close();
		} catch (IOException e)
		{
		}
	}
	
	@OnError
	public void onError(Session session, Throwable t)
	{
		onClose(session);
	}
	
	private class NodeUsageRunnable implements Runnable
	{
		private Session session;
		
		public NodeUsageRunnable(Session s)
		{
			this.session = s;
		}
		
		public void run()
		{
			var system = StartUpApplication.SYSTEM;
			while(session.isOpen() && !Thread.interrupted())
			{
				var cpuLoad = (int) (system.getSystemCpuLoad() * 100);
				var totalMemory = system.getTotalPhysicalMemorySize();
				var freeMemory = system.getFreePhysicalMemorySize();
				var ramUsage = (long) ((totalMemory - freeMemory) / ((double) totalMemory) * 100.0);
				if(cpuLoad >= 0)
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
					Thread.sleep(NODE_USAGE_WAIT_TIME);
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
