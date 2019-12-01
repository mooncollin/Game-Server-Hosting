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
	
	public static String getEndpoint(int serverID, int triggerID)
	{
		return String.format("%s?id=%d&triggerID=%d", URL, serverID, triggerID);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverIDStr = request.getParameter("id");
		var idStr = request.getParameter("triggerID");
		var serverID = Utils.fromString(Integer.class, serverIDStr);
		var triggerID = Utils.fromString(Integer.class, idStr);
		
		if(serverID == null || triggerID == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var redirectUrl = Utils.encodeURL(GameServerConsole.getEndpoint(serverID));
		
		var serverAddress = StartUpApplication.serverAddresses.get(serverID);
		if(serverAddress == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var url = Utils.encodeURL(String.format("http://%s%s", serverAddress, api.TriggerDelete.getEndpoint(triggerID)));
		
		try
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
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
