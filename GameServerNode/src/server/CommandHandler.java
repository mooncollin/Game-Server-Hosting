package server;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import api.minecraft.MinecraftServer;
import api.minecraft.MinecraftServerCommandHandler;

abstract public class CommandHandler <T>
{
	protected List<String[]> commands;
	protected T server;
	
	public CommandHandler(T server)
	{
		commands = new LinkedList<String[]>();
		
		this.server = Objects.requireNonNull(server);
	}
	
	public static String[] getCommand(Class<?> server, String command)
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
		
		for(var c : commands)
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
