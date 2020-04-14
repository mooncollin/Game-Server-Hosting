package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

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
		name = "GameServerFileDeleteMultiple",
		urlPatterns = "/GameServerFileDeleteMultiple",
		asyncSupported = true
)
/**
 * Backend endpoint for deleting multiple files at once for a particular game server.
 * Responsible for relaying the file properties to the corresponding node.
 * @author Collin
 *
 */
public class GameServerFileDeleteMultiple extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{	
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var directory = ApiSettings.DIRECTORY.parse(request);
		var files = ApiSettings.FILES.parse(request);
		if(!Utils.optionalsPresent(serverID, directory, files))
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
		
		var futures = new LinkedList<CompletableFuture<HttpResponse<Void>>>();
		
		for(var file : files.get())
		{
			var newList = new LinkedList<String>(directory.get());
			newList.add(file);
			var url = nodeapi.FileDelete.getEndpoint(newList);
			url.setHost(serverAddress);
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			futures.add(StartUpApplication.client.sendAsync(httpRequest, BodyHandlers.discarding()));
		}
		
		try
		{
			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		}
		catch(Exception e)
		{
			StartUpApplication.LOGGER.error(e.getMessage());
		}
		
		response.sendRedirect(redirectURL.getURL());
	}
}
