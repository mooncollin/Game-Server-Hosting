package server;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import api.minecraft.MinecraftServer;
import api.minecraft.MinecraftServerCommandHandler;

abstract public class CommandHandler
{
	protected List<String[]> commands;
	protected GameServer server;
	
	public CommandHandler(GameServer server)
	{
		commands = new LinkedList<String[]>();
		if(server == null)
		{
			throw new NullPointerException();
		}
		
		this.server = server;
	}
	
	public static String[] getCommand(Class<? extends GameServer> server, String command)
	{
		if(server == null || command == null)
		{
			return null;
		}
		
		List<String[]> commands;
		
		
		if(server.equals(GameServer.class))
		{
			commands = GameServerCommandHandler.COMMANDS;
		}
		else if(server.equals(MinecraftServer.class))
		{
			commands = MinecraftServerCommandHandler.COMMANDS;
		}
		else
		{
			return null;
		}
		
		for(String[] c : commands)
		{
			if(c.length > 0)
			{
				if(c[0].equals(command))
				{
					return c;
				}
			}
		}
		
		return null;
	}

	abstract public boolean commandGET(String command, HttpServletRequest request, HttpServletResponse response) throws IOException;
	abstract public boolean commandPOST(String command, HttpServletRequest request, HttpServletResponse response) throws IOException;
	
	public List<String[]> getCommands()
	{
		return Collections.unmodifiableList(commands);
	}
	
	public String[] getCommand(String command)
	{
		return CommandHandler.getCommand(server.getClass(), command);
	}
}
