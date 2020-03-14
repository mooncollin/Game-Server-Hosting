package nodeapi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import utils.Utils;

@ServerEndpoint("/Output")
public class Output
{
	
	private static final long WAIT_TIME = 50;
	public static final ExecutorService OUTPUT_THREADS = Executors.newCachedThreadPool();
	private static final Map<GameServer, Set<OutputContainer>> LISTENING_OUTPUT = new HashMap<GameServer, Set<OutputContainer>>();
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
		
		var container = new OutputContainer(session, outputOnly, runningOnly);
		
		synchronized(LISTENING_OUTPUT)
		{
			var absent = LISTENING_OUTPUT.putIfAbsent(foundServer, new HashSet<OutputContainer>());
			LISTENING_OUTPUT.get(foundServer).add(container);
			if(absent == null || LISTENING_OUTPUT.get(foundServer).size() == 1)
			{
				OUTPUT_THREADS.execute(() -> serveOutput(foundServer));
			}
		}
	}
	
	private static void serveOutput(GameServer foundServer)
	{
		Set<OutputContainer> outputs;
		var runningOnly = new HashSet<Session>();
		var outputOnly = new HashSet<Session>();
		var runningTracker = new HashMap<Session, Boolean>();
		synchronized (LISTENING_OUTPUT)
		{
			outputs = LISTENING_OUTPUT.get(foundServer);
		}
		if(outputs == null)
		{
			return;
		}
		
		var runningNotifier = foundServer.getRunningStateNotifier();
		var outputNotifier = foundServer.getOutputNotifier();
		var serverData = new ByteArrayOutputStream();
		foundServer.registerOutputConnector(serverData);
		
		while(!Thread.interrupted())
		{
			synchronized (outputs)
			{
				if(outputs.isEmpty())
				{
					return;
				}
				
				for(var output : outputs)
				{
					if(!output.session.isOpen())
					{
						runningOnly.remove(output.getSession());
						runningTracker.remove(output.getSession());
						outputOnly.remove(output.getSession());
					}
					else
					{
						if(output.isOutputOnly() || !output.isOutputOnly() && !output.isRunningOnly())
						{
							outputOnly.add(output.getSession());
						}
						if(output.isRunningOnly() || !output.isOutputOnly() && !output.isRunningOnly())
						{
							runningTracker.put(output.getSession(), null);
							runningOnly.add(output.getSession());
						}
					}
				}
			}
			
			synchronized (outputNotifier)
			{
				try
				{
					outputNotifier.wait(WAIT_TIME);
				} catch (InterruptedException e)
				{
					break;
				}
			}
			
			if(serverData.size() > 0)
			{
				for(var session : outputOnly)
				{
					if(session.isOpen())
					{
						synchronized (session)
						{
							try(var sessionStream = session.getBasicRemote().getSendStream())
							{
								serverData.writeTo(sessionStream);
							} catch (IOException e1)
							{
								return;
							}
						}
					}
				}
				serverData.reset();
			}
			
			synchronized (runningNotifier)
			{
				try
				{
					runningNotifier.wait(WAIT_TIME);
				} catch (InterruptedException e)
				{
					return;
				}
			}
			
			var run = foundServer.isRunning();
			var text = run ? SERVER_ON_MESSAGE : SERVER_OFF_MESSAGE;
			for(var session : runningOnly)
			{
				if(session.isOpen() && ( runningTracker.get(session) == null ||
				   run != runningTracker.get(session)))
				{
					synchronized (session)
					{
						try
						{
							session.getBasicRemote().sendText(text);
						} catch (IOException e)
						{
							return;
						}
					}
					runningTracker.replace(session, run);
				}
			}
		}
		
		foundServer.removeRunningStateNotifier(runningNotifier);
		foundServer.removeOutputNotifier(outputNotifier);
	}
	
	@OnClose
	public void onClose(Session session)
	{
		synchronized (LISTENING_OUTPUT)
		{
			LISTENING_OUTPUT.values()
				.stream()
				.map(Set::iterator)
				.forEach(it -> {
					while(it.hasNext())
					{
						var container = it.next();
						if(container.getSession().equals(session))
						{
							it.remove();
							break;
						}
					}
				});
		}
	}
	
	private static class OutputContainer
	{
		private final Session session;
		private final boolean outputOnly;
		private final boolean runningOnly;
		
		public OutputContainer(Session s, boolean outputOnly, boolean runningOnly)
		{
			this.session = s;
			this.outputOnly = outputOnly;
			this.runningOnly = runningOnly;
		}
		
		public Session getSession()
		{
			return session;
		}
		
		public boolean isOutputOnly()
		{
			return outputOnly;
		}
		
		public boolean isRunningOnly()
		{
			return runningOnly;
		}
	}
}
