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
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/GameServerFile")
public class GameServerFileDownload extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	public static final String URL = StartUpApplication.SERVLET_PATH +  "/GameServerFile";
	
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
	
	public static ParameterURL postEndpoint(int serverID, String directory)
	{
		return getEndpoint(serverID, directory);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		var directory = request.getParameter(ApiSettings.DIRECTORY_PARAMETER);
		if(serverID == null || directory == null || directory.isEmpty())
		{
			response.setStatus(400);
			return;
		}
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID);
		if(serverAddress == null)
		{
			response.setStatus(404);
			return;
		}
		
		var directories = directory.split(",");
		
		for(var d : directories)
		{
			if(d.contains(".."))
			{
				response.setStatus(400);
				return;
			}
		}
		
		var fileName = directories[directories.length - 1];
		if(fileName.indexOf('.') == -1)
		{
			fileName += ".zip";
		}
		
		response.setHeader("Content-disposition", "attachment; filename=\"" + fileName + "\"");
		
		try
		{
			var url = nodeapi.FileDownload.getEndpoint(directory);
			url.setHost(serverAddress);
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			var httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.ofInputStream());
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
