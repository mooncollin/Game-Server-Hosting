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
import nodeapi.ApiSettings;
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/GameServerTriggerDelete")
public class GameServerTriggerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameServerTriggerDelete";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL getEndpoint(int serverID, int triggerID)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID_PARAMETER, serverID);
		url.addQuery(ApiSettings.TRIGGER_ID_PARAMETER, triggerID);
		return url;
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		var triggerID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.TRIGGER_ID_PARAMETER));
		
		if(serverID == null || triggerID == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var redirectUrl = GameServerConsole.getEndpoint(serverID);
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID);
		if(serverAddress == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var url = nodeapi.TriggerDelete.getEndpoint(triggerID);
		url.setHost(serverAddress);
		
		try
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
		}
		catch(InterruptedException e)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		response.sendRedirect(redirectUrl.getURL());
	}
}
