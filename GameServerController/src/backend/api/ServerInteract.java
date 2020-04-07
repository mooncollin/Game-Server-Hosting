package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;

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
import utils.Utils;

@WebServlet(
		name = "ServerInteract",
		urlPatterns = "/ServerInteract",
		asyncSupported = true
)
@ServletSecurity(
		httpMethodConstraints = @HttpMethodConstraint(value = "GET")
)
public class ServerInteract extends HttpServlet
{
	private static final long serialVersionUID = 1L;
       
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var serverCommand = ApiSettings.COMMAND.parse(request);
		if(!Utils.optionalsPresent(serverID, serverCommand))
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		var serverAddress = StartUpApplication.getServerIPAddress(serverID.get());
		if(serverAddress == null)
		{
			response.setStatus(400);
			return;
		}
		
		var url = nodeapi.ServerInteract.getEndpoint(serverID.get(), serverCommand.get());
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
			var httpResponse = StartUpApplication.client.send(httpRequest, BodyHandlers.ofString());
			response.setStatus(httpResponse.statusCode());
			response.getWriter().print(httpResponse.body());
		} catch (InterruptedException e)
		{
			response.setStatus(500);
		}
	}
}
