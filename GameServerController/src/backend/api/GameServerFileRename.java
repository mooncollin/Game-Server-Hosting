package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.Endpoints;
import nodeapi.ApiSettings;

@WebServlet(
		name = "GameServerFileRename",
		urlPatterns = "/GameServerFileRename",
		asyncSupported = true
)
@ServletSecurity(
		httpMethodConstraints = @HttpMethodConstraint(value = "GET")
)
public class GameServerFileRename extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var directory = ApiSettings.DIRECTORY.parse(request);
		var rename = ApiSettings.RENAME.parse(request);
		var newFolder = ApiSettings.NEW_FOLDER.parse(request);

		if(directory.isEmpty() || (rename.isEmpty() && newFolder.isEmpty()) || directory.get().size() <= 1)
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
			var url = nodeapi.FileRename.getEndpoint(directory.get(), 
				rename.isPresent() ? rename.get() : newFolder.get(), rename.isEmpty());
			
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
			StartUpApplication.LOGGER.error(e.getMessage());
			response.setStatus(500);
			return;
		}
		
		response.sendRedirect(redirectURL.getURL());
	}
}
