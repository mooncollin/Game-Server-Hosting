package server;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GameServerCommandHandler<T extends GameServer> extends CommandHandler<T>
{
	public static final String START_COMMAND = "start";
	public static final String STOP_COMMAND = "stop";
	public static final String LOG_COMMAND = "log";
	public static final String RUNNING_COMMAND = "running";
	
	public static final List<String[]> COMMANDS = List.of
	(
		new String[] {START_COMMAND},
		new String[] {STOP_COMMAND},
		new String[] {LOG_COMMAND},
		new String[] {RUNNING_COMMAND}
	);
	
	public GameServerCommandHandler(T server)
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
		
		if(command.equals(START_COMMAND))
		{
			server.startServer();
		}
		else if(command.equals(STOP_COMMAND))
		{
			server.stopServer();
		}
		else if(command.equals(LOG_COMMAND))
		{
			response.getWriter().print(server.getLog());
		}
		else if(command.equals(RUNNING_COMMAND))
		{
			response.getWriter().print(server.isRunning() ? "yes" : "no");
		}
		else
		{
			return false;
		}
		
		response.setContentType("text/plain");
		return true;
	}
	
	public boolean commandPOST(String command, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		return false;
	}
}