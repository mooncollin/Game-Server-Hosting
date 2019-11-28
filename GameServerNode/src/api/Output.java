package api;

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
import server.GameServer;
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
	
	public static final String URL = "/Output";
	
	public static String getEndpoint(int id, String mode)
	{
		return String.format("%s?id=%d&mode=%s", URL, id, mode);
	}
	
	@OnOpen
	public void onOpen(Session session)
	{
		var query = session.getQueryString().split("=|&");
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
		
		if(!query[0].equals("id"))
		{
			try
			{
				session.close();
			} catch (IOException e)
			{
			}
			return;
		}
		
		int serverID;
		
		try
		{
			serverID = Integer.parseInt(query[1]);
		}
		catch(NumberFormatException e)
		{
			try
			{
				session.close();
			} catch (IOException e1)
			{
			}
			return;
		}
		
		var outputOnly = false;
		var runningOnly = false;
		
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
		
		GameServer foundServer = StartUpApplication.getServer(serverID);

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
