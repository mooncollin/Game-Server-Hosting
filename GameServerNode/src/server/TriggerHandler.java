package server;

import java.util.Objects;

abstract public class TriggerHandler
{
	public static final String START_SERVER = "Start Server";
	public static final String STOP_SERVER = "Stop Server";
	public static final String RESTART_SERVER = "Restart Server";
	
	public static final String RECURRING_TYPE = "recurring";
	public static final String TIME_TYPE = "time";
	public static final String OUTPUT_TYPE = "output";
	
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
		server.writeToServer(command);
		if(getAction().equals(START_SERVER))
		{
			server.startServer();
		}
		else if(getAction().equals(STOP_SERVER))
		{
			server.stopServer();
		}
		else if(getAction().equals(RESTART_SERVER))
		{
			server.stopServer();
			server.startServer();
		}
	}
}
