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

@WebServlet(
		name = "GameServerFileDelete",
		urlPatterns = "/GameServerFileDelete",
		asyncSupported = true
)
/**
 * Backend endpoint to delete a file pertaining to a certain game server.
 * Responsible for relaying the file properties to the corresponding node.
 * @author Collin
 *
 */
public class GameServerFileDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var directory = ApiSettings.DIRECTORY.parse(request);
		
		if(serverID.isEmpty() || directory.isEmpty())
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
		
		var redirectURL = Endpoints.GAME_SERVER_FILES.get(serverID.get(), directory.get().subList(0, directory.get().size() - 1));
		
		try
		{
			var url = nodeapi.FileDelete.getEndpoint(directory.get());
			url.setHost(serverAddress);
			
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			var httpResponse = StartUpApplication.client.send(httpRequest, BodyHandlers.ofString());
			if(httpResponse.statusCode() != 200)
			{
				response.sendRedirect(redirectURL.getURL());
				return;
			}
		}
		catch(InterruptedException e)
		{
			response.setStatus(500);
			return;
		}
		
		
		response.sendRedirect(redirectURL.getURL());
	}
}
