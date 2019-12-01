package api;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.StartUpApplication;
import utils.Utils;

@WebServlet("/ServerInteract")
public class ServerInteract extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/ServerInteract";
	
	public static String getEndpoint(int id, String command)
	{
		return String.format("%s?id=%d&command=%s", URL, id, command);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverIDStr = request.getParameter("id");
		var command = request.getParameter("command");
		var serverID = Utils.fromString(Integer.class, serverIDStr);
		
		if(serverIDStr == null || command == null || serverID == null)
		{
			response.setStatus(400);
			return;
		}
		
		var foundServer = StartUpApplication.getServer(serverID);
		
		if(foundServer == null)
		{
			response.setStatus(400);
			return;
		}
		
		if(!foundServer.getCommandHandler().commandGET(command, request, response))
		{
			response.setStatus(400);
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverIDStr = request.getParameter("id");
		var command = request.getParameter("command");
		var serverID = Utils.fromString(Integer.class, serverIDStr);
		
		if(serverIDStr == null || command == null || serverID == null)
		{
			response.setStatus(400);
			return;
		}
		
		var foundServer = StartUpApplication.getServer(serverID);
		
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
