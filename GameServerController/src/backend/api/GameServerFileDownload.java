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
import nodeapi.ApiSettings;

@WebServlet(
		name = "GameServerFile",
		urlPatterns = "/GameServerFile",
		asyncSupported = true
)
/**
 * Backend endpoint for downloading a file from a particular game server.
 * Responsible for asking the corresponding node for a particular file
 * and relaying that file back to the client.
 * @author Collin
 *
 */
public class GameServerFileDownload extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var directory = ApiSettings.DIRECTORY.parse(request);

		if(serverID.isEmpty() && directory.isEmpty())
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
		
		var fileName = directory.get().get(directory.get().size() - 1);
		if(!fileName.contains("."))
		{
			fileName += ".zip";
		}
		
		response.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
		
		try
		{
			var url = nodeapi.FileDownload.getEndpoint(directory.get());
			url.setHost(serverAddress);
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			var httpResponse = StartUpApplication.client.send(httpRequest, BodyHandlers.ofInputStream());
			if(httpResponse.statusCode() != 200)
			{
				response.setStatus(400);
				return;
			}
			
			httpResponse.body().transferTo(response.getOutputStream());
		}
		catch(InterruptedException e)
		{
			response.setStatus(500);
			return;
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
	}
}
