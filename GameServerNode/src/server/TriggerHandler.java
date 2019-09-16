package server;

import java.util.Objects;

abstract public class TriggerHandler
{
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
		if(getAction().equals("Start Server"))
		{
			server.startServer();
		}
		else if(getAction().equals("Stop Server"))
		{
			server.stopServer();
		}
		else if(getAction().equals("Restart Server"))
		{
			server.stopServer();
			server.startServer();
		}
	}
}
