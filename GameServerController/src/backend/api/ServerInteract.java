package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import server.CommandHandler;
import server.GameServer;
import utils.Pair;
import utils.Utils;

@WebServlet("/ServerInteract")
public class ServerInteract extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
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
		String serverName = request.getParameter("name");
		String serverCommand = request.getParameter("command");
		String[] extraCommands;
		if(serverName == null || serverCommand == null)
		{
			response.setStatus(400);
			return;
		}
		
		Pair<Class<? extends GameServer>, String> server = StartUpApplication.getServerInfo().get(serverName);
		if(server == null)
		{
			response.setStatus(400);
			return;
		}
		
		if(serverCommand.equals("output"))
		{
			response.getWriter().print(server.getSecond() + StartUpApplication.NODE_OUTPUT_URL);
			return;
		}
		
		String[] commands = CommandHandler.getCommand(server.getFirst(), serverCommand);
		if(commands == null)
		{	
			response.setStatus(400);
			return;
		}
		
		extraCommands = new String[commands.length - 1];
		for(int i = 1; i < commands.length; i++)
		{
			String command = request.getParameter(commands[i]);
			if(command == null)
			{
				response.setStatus(400);
				return;
			}
			extraCommands[i-1] = command;
		}
		
		String url = "http://" + server.getSecond() + "/ServerInteract?name=" + serverName + "&command=" + serverCommand;
		for(int i = 0; i < extraCommands.length; i++)
		{
			url += "&" + commands[i+1] + "=" + extraCommands[i];
		}
		
		HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(Utils.encodeURL(url))).build();
		
		try
		{
			HttpResponse<String> httpResponse = client.send(httpRequest, BodyHandlers.ofString());
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
