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
		name = "GameServerNewFolder",
		urlPatterns = "/GameServerNewFolder",
		asyncSupported = true
)
public class GameServerNewFolder extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var directory = ApiSettings.DIRECTORY.parse(request);
		var newFolder = ApiSettings.NEW_FOLDER.parse(request);
		
		if(!Utils.optionalsPresent(serverID, directory, newFolder) || directory.get().isEmpty())
		{
			response.setStatus(400);
			return;
		}
		
		var serverAddress = StartUpApplication.getServerIPAddress(serverID.get());
		if(serverAddress == null)
		{
			response.setStatus(404);
			return;
		}
		
		var redirectURL = Endpoints.GAME_SERVER_FILES.get(serverID.get(), directory.get());
		
		try
		{
			var url = nodeapi.Endpoints.NEW_FOLDER.get(directory.get(), newFolder.get());
			
			url.setHost(serverAddress);
			
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			var httpResponse = StartUpApplication.client.send(httpRequest, BodyHandlers.discarding());
			if(httpResponse.statusCode() != 200)
			{
				response.sendRedirect(redirectURL.getURL());
				return;
			}
		}
		catch(InterruptedException e)
		{
			StartUpApplication.LOGGER.error(e.getMessage());
			response.setStatus(500);
			return;
		}
		
		response.sendRedirect(redirectURL.getURL());
	}
}
