package nodeapi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import nodemain.StartUpApplication;
import server.GameServer;
import utils.ParameterURL;

@ServerEndpoint("/Output")
public class Output
{
	
	private static final long WAIT_TIME = 50;
	public static final ExecutorService OUTPUT_THREADS = Executors.newCachedThreadPool();
	private static final Map<GameServer, Set<Session>> SERVER_OUTPUT = new HashMap<GameServer, Set<Session>>();
	private static final Map<GameServer, Set<Session>> RUNNING_OUTPUT = new HashMap<GameServer, Set<Session>>();
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
		url.addQuery(ApiSettings.SERVER_ID.getName(), id);
		return url;
	}
	
	public static ParameterURL getEndpoint(int id, String mode)
	{
		var url = getEndpoint(id);
		url.addQuery(ApiSettings.OUTPUT_MODE.getName(), mode);
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
		
		var serverID = ApiSettings.SERVER_ID.parse(queryParameters.get(ApiSettings.SERVER_ID.getName()));
		var mode = ApiSettings.OUTPUT_MODE.parse(queryParameters.get(ApiSettings.OUTPUT_MODE.getName()));
		
		if(serverID.isEmpty())
		{
			try
			{
				session.close();
			} catch (IOException e)
			{
			}
			
			return;
		}
		
		var outputOnly = false;
		var runningOnly = false;
		
		
		if(!mode.isEmpty())
		{
			switch(mode.get())
			{
				case OUTPUT_VALUE:
					outputOnly = true;
					break;
				case RUNNING_VALUE:
					runningOnly = true;
					break;
			}
		}
		
		var foundServer = StartUpApplication.getServer(serverID.get());
		var neither = !outputOnly && !runningOnly;
		
		if(outputOnly || neither)
		{
			synchronized (SERVER_OUTPUT)
			{
				var set = SERVER_OUTPUT.computeIfAbsent(foundServer, k -> new HashSet<Session>());
				set.add(session);
				if(set.size() == 1)
				{
					OUTPUT_THREADS.execute(() -> serveOutput(foundServer));
				}
			}
		}
		if(runningOnly || neither)
		{
			synchronized (RUNNING_OUTPUT)
			{
				var set = RUNNING_OUTPUT.computeIfAbsent(foundServer, k -> new HashSet<Session>());
				set.add(session);
				if(set.size() == 1)
				{
					OUTPUT_THREADS.execute(() -> serveRunning(foundServer));
				}
			}
		}
	}
	
	private static void serveRunning(GameServer server)
	{
		var runningSessions = new HashMap<Session, Boolean>();
		var runningNotifier = server.getRunningStateNotifier();
		
		while(!Thread.interrupted())
		{
			synchronized (RUNNING_OUTPUT)
			{
				var set = RUNNING_OUTPUT.get(server);
				var it = runningSessions.entrySet().iterator();
				while(it.hasNext())
				{
					var entry = it.next();
					if(!set.contains(entry.getKey()))
					{
						it.remove();
					}
				}
				for(var session : set)
				{
					runningSessions.putIfAbsent(session, null);
				}
			}
			
			if(runningSessions.isEmpty())
			{
				break;
			}
			
			synchronized (runningNotifier)
			{
				try
				{
					runningNotifier.wait(WAIT_TIME);
				}
				catch(InterruptedException e)
				{
					break;
				}
			}
			
			var run = server.isRunning();
			var text = run ? SERVER_ON_MESSAGE : SERVER_OFF_MESSAGE;
			for(var entry : runningSessions.entrySet())
			{
				var session = entry.getKey();
				var result = Objects.requireNonNullElse(entry.getValue(), !run);
				if(run != result)
				{
					synchronized (session)
					{
						if(session.isOpen())
						{
							try
							{
								session.getBasicRemote().sendText(text);
							} catch (IOException e)
							{
								return;
							}
						}
					}
					runningSessions.replace(session, run);
				}
			}
		}
	}
	
	private static void serveOutput(GameServer server)
	{
		var sessions = new HashSet<Session>();
		var serverData = new ByteArrayOutputStream();
		server.registerOutputConnector(serverData);
		
		while(!Thread.interrupted())
		{
			synchronized (SERVER_OUTPUT)
			{
				var set = RUNNING_OUTPUT.get(server);
				sessions.addAll(set);
				sessions.retainAll(set);
			}
			
			if(sessions.isEmpty())
			{
				break;
			}
			
			synchronized (serverData)
			{
				try
				{
					serverData.wait(WAIT_TIME);
				}
				catch(InterruptedException e)
				{
					break;
				}
			}
			
			if(serverData.size() > 0)
			{
				for(var session : sessions)
				{
					synchronized (session)
					{
						if(session.isOpen())
						{
							try(var sessionStream = session.getBasicRemote().getSendStream())
							{
								serverData.writeTo(sessionStream);
							} catch (IOException e1)
							{
								continue;
							}
						}
					}
				}
				serverData.reset();
			}
		}
	}
	
	@OnClose
	public void onClose(Session session)
	{
		synchronized (SERVER_OUTPUT)
		{
			for(var values : SERVER_OUTPUT.values())
			{
				values.remove(session);
			}
		}
		
		synchronized (RUNNING_OUTPUT)
		{
			for(var values : RUNNING_OUTPUT.values())
			{
				values.remove(session);
			}
		}
	}
}
