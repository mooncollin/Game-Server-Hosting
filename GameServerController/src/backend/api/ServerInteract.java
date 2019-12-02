package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.time.Duration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.Index;
import nodeapi.ApiSettings;
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/ServerInteract")
public class ServerInteract extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/ServerInteract";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL getEndpoint(int serverID, String command)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID_PARAMETER, serverID);
		if(command != null && !command.isBlank())
		{
			url.addQuery(ApiSettings.COMMAND_PARAMETER, command);
		}
		return url;
	}
	
	public static ParameterURL postEndpoint(int serverID, String command)
	{
		return getEndpoint(serverID, command);
	}
	
	public static final HttpClient client;
	
	static
	{
		client = HttpClient.newBuilder()
				.version(Version.HTTP_1_1)
				.followRedirects(Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(20))
				.build();
	}
       
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		var serverCommand = request.getParameter(ApiSettings.COMMAND_PARAMETER);
		if(serverID == null || serverCommand == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID);
		if(serverAddress == null)
		{
			response.setStatus(400);
			return;
		}
		var url = nodeapi.ServerInteract.getEndpoint(serverID, serverCommand);
		url.setHost(serverAddress);
		
		for(var entry : request.getParameterMap().entrySet())
		{
			var name = entry.getKey();
			var value = entry.getValue()[0];
			url.addQuery(name, value);
		}
		
		var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();

		try
		{
			var httpResponse = client.send(httpRequest, BodyHandlers.ofString());
			response.setStatus(httpResponse.statusCode());
			response.getWriter().print(httpResponse.body());
		} catch (InterruptedException e)
		{
			response.setStatus(500);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
	}
}
