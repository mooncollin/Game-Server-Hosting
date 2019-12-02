package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.GameServerFiles;
import nodeapi.ApiSettings;
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/GameServerFileDeleteMultiple")
public class GameServerFileDeleteMultiple extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameServerFileDeleteMultiple";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL getEndpoint(int serverID, String directory, String files)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID_PARAMETER, serverID);
		url.addQuery(ApiSettings.DIRECTORY_PARAMETER, directory);
		url.addQuery(ApiSettings.FILES_PARAMETER, files);
		return url;
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		var directory = request.getParameter(ApiSettings.DIRECTORY_PARAMETER);
		var files = request.getParameter(ApiSettings.FILES_PARAMETER);
		if(serverID == null || directory == null || files == null)
		{
			response.setStatus(400);
			return;
		}
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID);
		if(serverAddress == null || directory.isEmpty() || files.isEmpty())
		{
			response.setStatus(404);
			return;
		}
		
		for(var d : directory.split(","))
		{
			if(d.contains(".."))
			{
				response.setStatus(400);
				return;
			}
		}
		
		var redirectURL = GameServerFiles.getEndpoint(serverID, directory);
		
		var futures = new LinkedList<CompletableFuture<HttpResponse<Void>>>();
		
		for(var file : files.split(","))
		{
			var url = nodeapi.FileDelete.getEndpoint(String.format("%s,%s", directory, file));
			url.setHost(serverAddress);
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			futures.add(ServerInteract.client.sendAsync(httpRequest, BodyHandlers.discarding()));
		}
		
		try
		{
			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		}
		catch(Exception e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
		}
		
		response.sendRedirect(redirectURL.getURL());
	}
}
