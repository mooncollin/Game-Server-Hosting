package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;
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
	
	public static ParameterURL getEndpoint(int serverID, String[] directories, String files)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
		url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", Arrays.asList(directories)));
		url.addQuery(ApiSettings.FILES.getName(), files);
		return url;
	}
	
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
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID.get());
		if(serverAddress == null)
		{
			response.setStatus(404);
			return;
		}
		
		var redirectURL = GameServerFiles.getEndpoint(serverID.get(), directory.get());
		
		var futures = new LinkedList<CompletableFuture<HttpResponse<Void>>>();
		
		for(var file : files.get())
		{
			var url = nodeapi.FileDelete.getEndpoint(Utils.concatenate(directory.get(), file));
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
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
		}
		
		response.sendRedirect(redirectURL.getURL());
	}
}
