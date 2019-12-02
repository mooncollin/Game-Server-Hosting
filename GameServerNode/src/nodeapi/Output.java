package nodeapi;

import java.io.IOException;
import java.util.HashMap;
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

import nodemain.StartUpApplication;
import utils.Pair;
import utils.ParameterURL;
import utils.Utils;

@ServerEndpoint("/Output")
public class Output
{
	
	private static final int MAXIUMUM_POOL_SIZE = 200;
	private static final int STARING_POOL_SIZE = 100;
	public static final ThreadPoolExecutor execService = new ThreadPoolExecutor(STARING_POOL_SIZE, MAXIUMUM_POOL_SIZE, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	private static final List<Pair<Session, Future<?>>> currentRunning = new LinkedList<Pair<Session, Future<?>>>();
	public static final String SERVER_ON_MESSAGE = "<on>";
	public static final String SERVER_OFF_MESSAGE = "<off>";
	
	public static final String URL = "/GameServerNode/Output";
	public static final String OUTPUT_VALUE = "output";
	public static final String RUNNING_VALUE = "running";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		ParameterURL.WEB_SOCKET_PROTOCOL, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL getEndpoint(int id)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID_PARAMETER, id);
		return url;
	}
	
	public static ParameterURL getEndpoint(int id, String mode)
	{
		var url = getEndpoint(id);
		url.addQuery(ApiSettings.OUTPUT_MODE_PARAMETER, mode);
		return url;
	}
	
	@OnOpen
	public void onOpen(Session session)
	{
		var query = session.getQueryString().split("=|&");
		var queryParameters = new HashMap<String, String>();
		for(var i = 0; i < query.length - 1; i += 2)
		{
			queryParameters.put(query[i], query[i+1]);
		}
		
		var serverID = Utils.fromString(Integer.class, queryParameters.get(ApiSettings.SERVER_ID_PARAMETER));
		var mode = queryParameters.get(ApiSettings.OUTPUT_MODE_PARAMETER);
		
		var outputOnly = false;
		var runningOnly = false;
		
		
		if(mode != null)
		{
			if(mode.equals(OUTPUT_VALUE))
			{
				outputOnly = true;
			}
			else if(mode.equals(RUNNING_VALUE))
			{
				runningOnly = true;
			}
		}
		
		var foundServer = StartUpApplication.getServer(serverID);

		if(outputOnly || !outputOnly && !runningOnly)
		{
			var future = execService.submit(foundServer.getOutputRunnable(session));
			synchronized(currentRunning)
			{
				currentRunning.add(Pair.of(session, future));
			}
		}
		if(runningOnly || !outputOnly && !runningOnly)
		{
			var future = execService.submit(foundServer.getServerRunningStatusRunnable(session));
			synchronized (currentRunning)
			{
				currentRunning.add(Pair.of(session, future));
			}
		}
	}
	
	@OnClose
	public void onClose(Session session)
	{
		synchronized(currentRunning)
		{
			currentRunning.removeIf(pair -> {
				var currentSession = pair.getFirst();
				var currentRunnable = pair.getSecond();
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
		onClose(session);
	}
}
