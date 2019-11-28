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
import utils.Utils;

@WebServlet("/GameServerFile")
public class GameServerFileDownload extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	public static final String URL = "/GameServerController/GameServerFile";
	
	public static String getEndpoint(int serverID, String directory)
	{
		return String.format("%s?id=%d&directory=%s", URL, serverID, directory);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverIDStr = request.getParameter("id");
		String directory = request.getParameter("directory");
		if(serverIDStr == null || directory == null || directory.isEmpty())
		{
			response.setStatus(400);
			return;
		}
		
		int serverID;
		try
		{
			serverID = Integer.parseInt(serverIDStr);
		}
		catch(NumberFormatException e)
		{
			response.setStatus(400);
			return;
		}
		
		var serverAddress = StartUpApplication.serverAddresses.get(serverID);
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
			var url = String.format("http://%s%s", serverAddress, Utils.encodeURL(api.FileDownload.getEndpoint(directory)));
			var httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
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
