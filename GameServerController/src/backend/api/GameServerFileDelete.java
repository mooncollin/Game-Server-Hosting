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
import utils.Utils;

@WebServlet("/GameServerFileDelete")
public class GameServerFileDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerFileDelete";
	
	public static String getEndpoint(int serverID, String directory)
	{
		return String.format("%s?id=%d&directory=%s", URL, serverID, directory);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverIDStr = request.getParameter("id");
		var directory = request.getParameter("directory");
		if(serverIDStr == null || directory == null)
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
		
		var redirectURL = Utils.encodeURL(GameServerFiles.getEndpoint(serverID, directory.substring(0, directory.lastIndexOf(','))));
		
		try
		{
			var url = Utils.encodeURL(String.format("http://%s%s", serverAddress, api.FileDelete.getEndpoint(directory)));
			var httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
			var httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.ofString());
			if(httpResponse.statusCode() != 200)
			{
				response.sendRedirect(redirectURL);
				return;
			}
		}
		catch(InterruptedException e)
		{
			response.setStatus(500);
			return;
		}
		
		
		response.sendRedirect(redirectURL);
	}
}
