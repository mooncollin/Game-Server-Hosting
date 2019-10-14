package api;

import java.io.IOException;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.NodeProperties;
import main.StartUpApplication;
import server.GameServer;

@WebServlet("/ServerInteract")
public class ServerInteract extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
		String command = request.getParameter("command");
		
		
		if(serverName == null || command == null)
		{
			StartUpApplication.LOGGER.log(Level.WARNING, String.format("Node: %s, ServerInteract: Invalid server name or command", NodeProperties.NAME));
			response.setStatus(400);
			return;
		}
		
		GameServer foundServer = StartUpApplication.getServer(serverName);
		
		if(foundServer == null)
		{
			StartUpApplication.LOGGER.log(Level.WARNING, String.format("Node: %s, ServerInteract: Invalid server", NodeProperties.NAME));
			response.setStatus(400);
			return;
		}
		
		if(!foundServer.getCommandHandler().commandGET(command, request, response))
		{
			StartUpApplication.LOGGER.log(Level.WARNING, String.format("Node: %s, ServerInteract: Invalid command", NodeProperties.NAME));
			response.setStatus(400);
		}
		
		response.setContentType("text/plain");
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
		String command = request.getParameter("command");
		
		if(serverName == null || command == null)
		{
			response.setStatus(400);
			return;
		}
		
		GameServer foundServer = StartUpApplication.getServer(serverName);
		
		if(foundServer == null)
		{
			response.setStatus(400);
			return;
		}
		
		if(!foundServer.getCommandHandler().commandPOST(command, request, response))
		{
			response.setStatus(400);
		}
	}
}
