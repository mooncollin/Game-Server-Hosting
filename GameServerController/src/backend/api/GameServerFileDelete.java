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
import frontend.GameServerFiles;
import nodeapi.ApiSettings;
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/GameServerFileDelete")
public class GameServerFileDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameServerFileDelete";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL getEndpoint(int serverID, String directory)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID_PARAMETER, serverID);
		url.addQuery(ApiSettings.DIRECTORY_PARAMETER, directory);
		return url;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		var directory = request.getParameter(ApiSettings.DIRECTORY_PARAMETER);
		if(serverID == null || directory == null)
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
		
		var redirectURL = GameServerFiles.getEndpoint(serverID, directory.substring(0, directory.lastIndexOf(',')));
		
		try
		{
			var url = nodeapi.FileDelete.getEndpoint(directory);
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
			response.setStatus(500);
			return;
		}
		
		
		response.sendRedirect(redirectURL.getURL());
	}
}
