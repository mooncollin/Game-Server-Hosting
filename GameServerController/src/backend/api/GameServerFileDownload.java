package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Arrays;

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
	
	public static ParameterURL getEndpoint(int serverID, String[] directories)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
		url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", Arrays.asList(directories)));
		return url;
	}
	
	public static ParameterURL postEndpoint(int serverID, String[] directories)
	{
		return getEndpoint(serverID, directories);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var directory = ApiSettings.DIRECTORY.parse(request);

		if(serverID.isEmpty() && directory.isEmpty())
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
		
		var fileName = Utils.lastOf(directory.get(), 1);
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
