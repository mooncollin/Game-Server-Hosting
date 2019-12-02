package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
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

@WebServlet("/GameServerFileRename")
public class GameServerFileRename extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameServerFileRename";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL getEndpoint(int serverID, String directory, String rename, boolean newFolder)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID_PARAMETER, serverID);
		url.addQuery(ApiSettings.DIRECTORY_PARAMETER, directory);
		
		if(rename != null && !rename.isEmpty())
		{
			url.addQuery(newFolder ? ApiSettings.NEW_FOLDER_PARAMETER : ApiSettings.RENAME_PARAMETER, rename);
		}
		
		return url;
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		var directory = request.getParameter(ApiSettings.DIRECTORY_PARAMETER);
		var rename = request.getParameter(ApiSettings.RENAME_PARAMETER);
		var newFolder = request.getParameter(ApiSettings.NEW_FOLDER_PARAMETER);
		if(serverID == null || directory == null || (rename == null && newFolder == null))
		{
			response.setStatus(400);
			return;
		}
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID);
		if(serverAddress == null || directory.isEmpty())
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
		
		var lastIndex = directory.lastIndexOf(',');
		if(lastIndex == -1 || newFolder != null)
		{
			lastIndex = directory.length();
		}
		
		var redirectURL = GameServerFiles.getEndpoint(serverID, directory.substring(0, lastIndex));
		
		try
		{
			var url = nodeapi.FileRename.getEndpoint(directory, 
				rename != null ? rename : newFolder, rename == null);
			
			url.setHost(serverAddress);
			
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			var httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.ofString());
			if(httpResponse.statusCode() != 200)
			{
				response.sendRedirect(redirectURL.getURL());
				return;
			}
		}
		catch(InterruptedException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
		
		response.sendRedirect(redirectURL.getURL());
	}
}
