package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import nodeapi.ApiSettings;
import utils.Utils;
import utils.servlet.HttpStatus;

@WebServlet(
		name = "ServerInteract",
		urlPatterns = "/ServerInteract",
		asyncSupported = true
)
/**
 * Backend endpoint for the Command Handling framework messaging to relay messages
 * back and forth between generic and custom server implementations on the node
 * application. This endpoint is not responsible for the already existing web socket
 * usage for getting node resource info, real-time server on/off updates, and real-time
 * server output. However, this still can be used to check if a server is on/off and get
 * the last log of the server.
 * @author Collin
 *
 */
public class ServerInteract extends HttpServlet
{
	private static final long serialVersionUID = 1L;
       
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		if(!Utils.optionalsPresent(serverID))
		{
			response.setStatus(HttpStatus.BAD_REQUEST.getCode());
			return;
		}
		
		var serverAddress = StartUpApplication.getServerIPAddress(serverID.get());
		if(serverAddress == null)
		{
			response.setStatus(HttpStatus.BAD_REQUEST.getCode());
			return;
		}
		
		var url = nodeapi.ServerInteract.postEndpoint(serverID.get());
		url.setHost(serverAddress);
		
		var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL()))
									 .POST(BodyPublishers.ofInputStream(() ->
									{
										try
										{
											return request.getInputStream();
										} catch (IOException e1)
										{
											throw new RuntimeException();
										}
									}))
									 .build();

		try
		{
			var httpResponse = StartUpApplication.client.send(httpRequest, BodyHandlers.ofString());
			response.setStatus(httpResponse.statusCode());
			response.setContentType("application/json");
			response.getWriter().print(httpResponse.body());
		} catch (InterruptedException e)
		{
			response.setStatus(500);
		}
	}
}
