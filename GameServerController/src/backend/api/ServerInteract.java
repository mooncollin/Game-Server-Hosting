package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.time.Duration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.Index;
import server.CommandHandler;
import utils.Utils;

@WebServlet("/ServerInteract")
public class ServerInteract extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/ServerInteract";
	
	public static String getEndpoint(int serverID, String command)
	{
		if(command == null || command.isBlank())
		{
			return String.format("%s?id=%d", URL, serverID);
		}
		
		return String.format("%s?id=%d&command=%s", URL, serverID, command);
	}
	
	public static final HttpClient client;
	
	static
	{
		client = HttpClient.newBuilder()
				.version(Version.HTTP_1_1)
				.followRedirects(Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(20))
				.build();
	}
       
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverIDStr = request.getParameter("id");
		var serverCommand = request.getParameter("command");
		String[] extraCommands;
		if(serverIDStr == null || serverCommand == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		int serverID;
		try
		{
			serverID = Integer.parseInt(serverIDStr);
		}
		catch(NumberFormatException e)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var serverAddress = StartUpApplication.serverAddresses.get(serverID);
		var serverType = StartUpApplication.serverTypes.get(serverID);
		if(serverAddress == null)
		{
			response.setStatus(400);
			return;
		}
		
		if(serverCommand.equals("output"))
		{
			response.getWriter().print(serverAddress + StartUpApplication.NODE_OUTPUT_URL);
			return;
		}
		
		var commands = CommandHandler.getCommand(serverType, serverCommand);
		if(commands == null)
		{
			response.setStatus(400);
			return;
		}
		
		extraCommands = new String[commands.length - 1];
		for(var i = 1; i < commands.length; i++)
		{
			var command = request.getParameter(commands[i]);
			if(command == null)
			{
				response.setStatus(400);
				return;
			}
			extraCommands[i-1] = command;
		}
		
		var url = String.format("http://%s%s", serverAddress, api.ServerInteract.getEndpoint(serverID, serverCommand));
		for(var i = 0; i < extraCommands.length; i++)
		{
			url += "&" + commands[i+1] + "=" + extraCommands[i];
		}
		
		var httpRequest = HttpRequest.newBuilder(URI.create(Utils.encodeURL(url))).build();

		try
		{
			var httpResponse = client.send(httpRequest, BodyHandlers.ofString());
			response.setStatus(httpResponse.statusCode());
			response.getWriter().print(httpResponse.body());
		} catch (InterruptedException e)
		{
			response.setStatus(500);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
	}
}
