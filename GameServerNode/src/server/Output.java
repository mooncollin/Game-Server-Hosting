package server;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import main.StartUpApplication;
import utils.Pair;

@ServerEndpoint("/Output")
public class Output
{
	private static final int MAXIUMUM_POOL_SIZE = 200;
	private static final int STARING_POOL_SIZE = 100;
	public static final ThreadPoolExecutor execService = new ThreadPoolExecutor(STARING_POOL_SIZE, MAXIUMUM_POOL_SIZE, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	private static final List<Pair<Session, Future<?>>> currentRunning = Collections.synchronizedList(new LinkedList<Pair<Session, Future<?>>>());
	public static final String SERVER_ON_MESSAGE = "<on>";
	public static final String SERVER_OFF_MESSAGE = "<off>";
	public static final String URL = "GameServerNode/Output";
	
	@OnOpen
	public void onOpen(Session session)
	{
		String[] query = session.getQueryString().split("=|&");
		if(query.length < 2)
		{
			try
			{
				session.close();
			} catch (IOException e)
			{
			}
			return;
		}
		
		if(!query[0].equals("name"))
		{
			try
			{
				session.close();
			} catch (IOException e)
			{
			}
			return;
		}
		
		String serverName = query[1].replace('+', ' ');
		boolean outputOnly = false;
		boolean runningOnly = false;
		
		if(query.length > 3)
		{
			if(query[2].equals("mode"))
			{
				if(query[3].equals("output"))
				{
					outputOnly = true;
				}
				else if(query[3].equals("running"))
				{
					runningOnly = true;
				}
			}
		}
		
		GameServer foundServer = StartUpApplication.getServer(serverName);

		if(outputOnly || !outputOnly && !runningOnly)
		{
			Future<?> future = execService.submit(foundServer.getOutputRunnable(session));
			currentRunning.add(Pair.of(session, future));
		}
		if(runningOnly || !outputOnly && !runningOnly)
		{
			Future<?> future = execService.submit(foundServer.getServerRunningStatusRunnable(session));
			currentRunning.add(Pair.of(session, future));
		}
	}
	
	@OnClose
	public void onClose(Session session)
	{
		synchronized(currentRunning)
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
		try
		{
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
}
