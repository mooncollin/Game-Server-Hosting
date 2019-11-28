package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.GameServerFiles;
import utils.Utils;

@WebServlet("/GameServerFileRename")
public class GameServerFileRename extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerFileRename";
	
	public static String getEndpoint(int serverID, String directory, String rename, boolean newFolder)
	{
		if(rename.isEmpty())
		{
			return String.format("%s?id=%d&directory=%s", URL, serverID, directory);
		}
		
		return String.format("%s?id=%d&directory=%s&%s=%s", URL, serverID, directory, 
			newFolder ? "newFolder" : "rename", rename);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverIDStr = request.getParameter("id");
		var directory = request.getParameter("directory");
		var rename = request.getParameter("rename");
		var newFolder = request.getParameter("newFolder");
		if(serverIDStr == null || directory == null || (rename == null && newFolder == null))
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
		
		var lastIndex = directory.lastIndexOf(',');
		if(lastIndex == -1 || newFolder != null)
		{
			lastIndex = directory.length();
		}
		
		var redirectURL = GameServerFiles.getEndpoint(serverID, directory.substring(0, lastIndex));
		
		try
		{
			var url = String.format("http://%s%s", serverAddress, api.FileRename.getEndpoint(directory, 
				rename != null ? rename.replace(' ', '+') : newFolder.replace(' ',  '+'),
				rename == null));
			
			var httpRequest = HttpRequest.newBuilder(URI.create(Utils.encodeURL(url))).build();
			var httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.ofString());
			if(httpResponse.statusCode() != 200)
			{
				response.sendRedirect(redirectURL);
				return;
			}
		}
		catch(InterruptedException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
		
		response.sendRedirect(redirectURL);
	}
}
