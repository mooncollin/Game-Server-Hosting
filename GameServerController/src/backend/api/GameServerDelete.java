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
import frontend.Index;
import nodeapi.ApiSettings;
import utils.ParameterURL;

@WebServlet("/GameServerDelete")
public class GameServerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameServerDelete";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL getEndpoint(int id)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID.getName(), id);
		return url;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		if(serverID.isEmpty())
		{
			response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
			return;
		}
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID.get());
		if(serverAddress == null)
		{
			response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
			return;
		}
		
		var url = nodeapi.ServerDelete.getEndpoint(serverID.get());
		url.setHost(serverAddress);
		
		try
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			var httpResponse = StartUpApplication.client.send(httpRequest, BodyHandlers.discarding());
			if(httpResponse.statusCode() == 200)
			{
				StartUpApplication.serverTypes.remove(serverID.get());
				StartUpApplication.serverIPAddresses.remove(serverID.get());
			}
		}
		catch(InterruptedException e)
		{
			
		}
		
		response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
	}
}
