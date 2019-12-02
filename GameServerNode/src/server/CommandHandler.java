package server;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

abstract public class CommandHandler <T>
{
	protected List<String[]> commands;
	protected T server;
	
	public CommandHandler(T server)
	{
		commands = new LinkedList<String[]>();
		
		this.server = Objects.requireNonNull(server);
	}

	abstract public boolean commandGET(String command, HttpServletRequest request, HttpServletResponse response) throws IOException;
	abstract public boolean commandPOST(String command, HttpServletRequest request, HttpServletResponse response) throws IOException;
	
	public List<String[]> getCommands()
	{
		return Collections.unmodifiableList(commands);
	}
	
	public String[] getCommand(String command)
	{
		for(var c : commands)
		{
			if(c.length > 0 && c[0].equals(command))
			{
				return c;
			}
		}
		return null;
	}
}
