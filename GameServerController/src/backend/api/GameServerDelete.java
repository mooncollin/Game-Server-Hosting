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
		name = "GameServerDelete",
		urlPatterns = "/GameServerDelete",
		asyncSupported = true
)
@ServletSecurity(
		httpMethodConstraints = @HttpMethodConstraint(value = "GET")
)
public class GameServerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		if(serverID.isEmpty())
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		var serverAddress = StartUpApplication.getServerIPAddress(serverID.get());
		if(serverAddress == null)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
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
				StartUpApplication.removeServerIPAddress(serverID.get());
			}
		}
		catch(InterruptedException e)
		{
			
		}
		
		response.sendRedirect(Endpoints.INDEX.get().getURL());
	}
}
