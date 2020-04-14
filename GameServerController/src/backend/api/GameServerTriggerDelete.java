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
import frontend.Endpoints;
import nodeapi.ApiSettings;
import utils.Utils;

@WebServlet(
		name = "GameServerTriggerDelete",
		urlPatterns = "/GameServerTriggerDelete",
		asyncSupported = true
)
/**
 * Backend endpoint for deleting a trigger. Responsible for deleting the trigger
 * from the database and relaying to the corresponding node that a trigger is not
 * longer in use.
 * @author Collin
 *
 */
public class GameServerTriggerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{		
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var triggerID = ApiSettings.TRIGGER_ID.parse(request);
		
		if(!Utils.optionalsPresent(serverID, triggerID))
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		var serverAddress = StartUpApplication.getServerIPAddress(serverID.get());
		if(serverAddress == null)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		var redirectUrl = Endpoints.GAME_SERVER_CONSOLE.get(serverID.get());
		
		var url = nodeapi.TriggerDelete.getEndpoint(triggerID.get());
		url.setHost(serverAddress);
		
		try
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			StartUpApplication.client.send(httpRequest, BodyHandlers.discarding());
		}
		catch(InterruptedException e)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		response.sendRedirect(redirectUrl.getURL());
	}
}
