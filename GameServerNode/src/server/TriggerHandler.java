package server;

import java.io.IOException;
import java.util.Objects;

import nodemain.StartUpApplication;

abstract public class TriggerHandler
{
	public static final String START_SERVER = "Start Server";
	public static final String STOP_SERVER = "Stop Server";
	public static final String RESTART_SERVER = "Restart Server";
	
	public static final String RECURRING_TYPE = "Recurring";
	public static final String TIME_TYPE = "Time";
	public static final String OUTPUT_TYPE = "Output";
	
	private GameServer server;
	private String command;
	private String action;
	private long id;
	
	public TriggerHandler(GameServer server, String command, String action, long id)
	{
		setServer(server);
		setCommand(command);
		setAction(action);
		setID(id);
	}
	
	public void setServer(GameServer server)
	{
		this.server = Objects.requireNonNull(server, "Server cannot be null");
	}
	
	public void setAction(String action)
	{
		this.action = Objects.requireNonNullElse(action, "");
	}
	
	public void setCommand(String command)
	{		
		this.command = Objects.requireNonNullElse(command, "");
	}
	
	public GameServer getServer()
	{
		return server;
	}
	
	public String getCommand()
	{
		return command;
	}
	
	public void setID(long id)
	{
		this.id = id;
	}
	
	public long getID()
	{
		return id;
	}
	
	public String getAction()
	{
		return action;
	}
	
	public void trigger()
	{
		try
		{
			server.writeToServer(command);
		} catch (IOException e)
		{
			StartUpApplication.LOGGER.error(String.format("Error writing to server:\n%s", e.getMessage()));
			return;
		}
		
		if(getAction().equals(START_SERVER))
		{
			try
			{
				server.startServer();
			}
			catch(IOException e)
			{
				StartUpApplication.LOGGER.error(String.format("Error starting server:\n%s", e.getMessage()));
				return;
			}
		}
		else if(getAction().equals(STOP_SERVER))
		{
			try
			{
				server.stopServer();
			} catch (IOException e)
			{
				StartUpApplication.LOGGER.error(String.format("Error stopping server:\n%s", e.getMessage()));
				return;
			}
		}
		else if(getAction().equals(RESTART_SERVER))
		{
			try
			{
				server.restartServer();
			}
			catch(IOException e)
			{
				StartUpApplication.LOGGER.error(String.format("Error restarting server:\n%s", e.getMessage()));
			}
		}
	}
}
