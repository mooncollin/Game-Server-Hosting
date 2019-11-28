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

import frontend.Index;
import utils.Utils;
import backend.main.StartUpApplication;

@WebServlet("/GameServerDelete")
public class GameServerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerDelete";
	
	public static String getEndpoint(int id)
	{
		return String.format("%s?id=%d", URL, id);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverIDStr = request.getParameter("id");
		if(serverIDStr == null)
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
		if(serverAddress == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		final var url = Utils.encodeURL(String.format("http://%s%s", serverAddress, api.ServerDelete.getEndpoint(serverID)));
		
		try
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
			var httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
			if(httpResponse.statusCode() == 200)
			{
				StartUpApplication.serverTypes.remove(serverID);
				StartUpApplication.serverAddresses.remove(serverID);
			}
		}
		catch(InterruptedException e)
		{
			
		}
		
		response.sendRedirect(Index.URL);
	}
}
