package api.minecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import main.StartUpApplication;
import server.GameServer;
import utils.Pair;
import utils.Utils;

@ServerEndpoint("/Output")
public class Output
{
	private static final int MAXIUMUM_POOL_SIZE = 200;
	private static final int STARING_POOL_SIZE = 100;
	private static final int MAXIUMUM_WAITING_TIME = 2000;
	private static final int MINIUMUM_WAITING_TIME = 100;
	public static final ThreadPoolExecutor execService = new ThreadPoolExecutor(STARING_POOL_SIZE, MAXIUMUM_POOL_SIZE, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	private static final List<Pair<Session, Future<?>>> currentRunning = Collections.synchronizedList(new LinkedList<Pair<Session, Future<?>>>());
	public static final String SERVER_ON_MESSAGE = "<on>";
	public static final String SERVER_OFF_MESSAGE = "<off>";
	public static final String URL = "GameServerNode/Output";
	private static int waitTime = MAXIUMUM_WAITING_TIME;
	
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
		if(!(foundServer instanceof MinecraftServer))
		{
			try
			{
				session.close();
			} catch (IOException e)
			{
			}
			return;
		}

		if(outputOnly || !outputOnly && !runningOnly)
		{
			Future<?> future = execService.submit(new OutputRunnable(session, (MinecraftServer) foundServer));
			currentRunning.add(Pair.of(session, future));
		}
		if(runningOnly || !outputOnly && !runningOnly)
		{
			Future<?> future = execService.submit(new ServerRunningStatusRunnable(session, foundServer));
			currentRunning.add(Pair.of(session, future));
		}
		
		waitTime = MAXIUMUM_WAITING_TIME - Utils.map(execService.getActiveCount(), 0, MAXIUMUM_POOL_SIZE, MINIUMUM_WAITING_TIME, MAXIUMUM_WAITING_TIME);
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
		
		waitTime = MAXIUMUM_WAITING_TIME - Utils.map(execService.getActiveCount(), 0, MAXIUMUM_POOL_SIZE, MINIUMUM_WAITING_TIME, MAXIUMUM_WAITING_TIME);
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
	
	private class OutputRunnable implements Runnable
	{
		private Session session;
		private MinecraftServer server;
		private PipedOutputStream serverOut;
		private PipedInputStream pipedIn;
		private BufferedReader clientIn;
		private Object notifier;
		
		public OutputRunnable(Session s, MinecraftServer server)
		{
			this.session = s;
			this.server = server;
			resetPipes();
			server.getOutputConnectors().add(serverOut);
			notifier = server.getRunningStateNotifier();
		}
		
		private void resetPipes()
		{
			if(serverOut != null)
			{
				try
				{
					serverOut.close();
					server.getOutputConnectors().remove(serverOut);
				} catch (IOException e)
				{
					e.printStackTrace();
					return;
				}
			}
			if(pipedIn != null)
			{
				try
				{
					pipedIn.close();
					clientIn.close();
				} catch (IOException e)
				{
					e.printStackTrace();
					return;
				}
			}
			serverOut = new PipedOutputStream();
			pipedIn = new PipedInputStream();
			try
			{
				serverOut.connect(pipedIn);
			} catch (IOException e)
			{
				e.printStackTrace();
				return;
			}
			clientIn = new BufferedReader(new InputStreamReader(pipedIn));
			server.getOutputConnectors().add(serverOut);
		}
		
		public void run()
		{
			String lastLine = null;
			while(session.isOpen() && !Thread.currentThread().isInterrupted())
			{
				try
				{
					String output;
					if(!session.isOpen() || Thread.currentThread().isInterrupted())
					{
						break;
					}
					
					while(clientIn.ready())
					{
						synchronized(clientIn)
						{
							output = clientIn.readLine();
						}
						if(output == null || lastLine != null && lastLine.equals(output))
						{
							continue;
						}
						lastLine = output;
						session.getBasicRemote().sendText(output + "\r\n");
					}
					
					Thread.sleep(waitTime);
				} catch (IOException | IllegalStateException | InterruptedException e)
				{
					StartUpApplication.LOGGER.log(Level.WARNING, String.format("Failed to send output data to server '%s':\n%s", server.getName(), e.getMessage()));
					break;
				}
			}
			
			List<OutputStream> serverConnectors = server.getOutputConnectors();
			
			synchronized(serverConnectors)
			{
				if(!serverConnectors.remove(serverOut))
				{
					StartUpApplication.LOGGER.log(Level.WARNING, String.format("Unable to remove output connector from server '%s'", server.getName()));
				}
			}
			server.removeRunningStateNotifier(notifier);
			
			try
			{
				serverOut.close();
				clientIn.close();
			} catch (IOException e)
			{
			}
		}
	}
	
	private class ServerRunningStatusRunnable implements Runnable
	{
		private Session session;
		private GameServer server;
		private Object notifier;
		
		public ServerRunningStatusRunnable(Session s, GameServer server)
		{
			this.session = s;
			this.server = server;
			this.notifier = server.getRunningStateNotifier();
		}
		
		public void run()
		{
			Boolean lastSent = null;
			while(session.isOpen() && !Thread.currentThread().isInterrupted())
			{
				try
				{
					if(lastSent != null)
					{
						if(lastSent.booleanValue() == server.isRunning())
						{
							Thread.sleep(500);
							continue;
						}
						synchronized (notifier)
						{
							notifier.wait(waitTime);
						}
						if(!session.isOpen() || Thread.currentThread().isInterrupted())
						{
							break;
						}
					}
				}
				catch(InterruptedException e)
				{
					break;
				}
				
				lastSent = server.isRunning();
				String text = lastSent ? SERVER_ON_MESSAGE : SERVER_OFF_MESSAGE;
				try
				{
					session.getBasicRemote().sendText(text);
				} catch (IOException e)
				{
					StartUpApplication.LOGGER.log(Level.WARNING, String.format("Failed to send running data from server '%s':\n%s", server.getName(), e.getMessage()));
					break;
				}
			}
			
			server.removeRunningStateNotifier(this.notifier);
		}
	}
}
