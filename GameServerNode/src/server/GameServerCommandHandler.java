package server;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GameServerCommandHandler extends CommandHandler
{
	public static final List<String[]> COMMANDS = List.of
	(
		new String[] {"start"},
		new String[] {"stop"},
		new String[] {"log"},
		new String[] {"running"}
	);
	
	public GameServerCommandHandler(GameServer server)
	{
		super(server);
		commands = new LinkedList<String[]>(COMMANDS);
	}
	
	public boolean commandGET(String command, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		if(command == null || request == null || response == null)
		{
			return false;
		}
		
		if(command.equals("start"))
		{
			server.startServer();
		}
		else if(command.equals("stop"))
		{
			server.stopServer();
		}
		else if(command.equals("log"))
		{
			response.getWriter().print(server.getLog());
		}
		else if(command.equals("running"))
		{
			response.getWriter().print(server.isRunning() ? "yes" : "no");
		}
		else
		{
			return false;
		}
		
		return true;
	}
	
	public boolean commandPOST(String command, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		return false;
	}
}