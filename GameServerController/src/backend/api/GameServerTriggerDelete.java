package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.GameServerConsole;
import frontend.Index;
import utils.Utils;

@WebServlet("/GameServerTriggerDelete")
public class GameServerTriggerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerTriggerDelete";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
		String idStr = request.getParameter("id");
		int id;
		
		final String redirectUrl = Utils.encodeURL(String.format("%s?name=%s", GameServerConsole.URL, serverName));
		
		if(idStr == null || serverName == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		try
		{
			id = Integer.valueOf(idStr);
		}
		catch(NumberFormatException e)
		{
			response.sendRedirect(redirectUrl);
			return;
		}
		
		var foundServer = StartUpApplication.getServerInfo().get(serverName);
		if(foundServer == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		final String url = Utils.encodeURL(String.format("http://%s/TriggerDelete?id=%s", foundServer.getSecond(), id));
		
		try
		{
			HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
			ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
		}
		catch(InterruptedException e)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		response.sendRedirect(redirectUrl);
	}
}
