package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.GameServerFiles;
import utils.Utils;

@WebServlet("/GameServerFileDeleteMultiple")
public class GameServerFileDeleteMultiple extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerFileDeleteMultiple";
	
	public static String getEndpoint(int serverID, String directory, String files)
	{
		return String.format("%s?id=%d&directory=%s&files=%s", URL, serverID, directory, files);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverIDStr = request.getParameter("id");
		var directory = request.getParameter("directory");
		var files = request.getParameter("files");
		if(serverIDStr == null || directory == null || files == null)
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
		if(serverAddress == null || directory.isEmpty() || files.isEmpty())
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
		
		var redirectURL = Utils.encodeURL(GameServerFiles.getEndpoint(serverID, directory));
		
		var futures = new LinkedList<CompletableFuture<HttpResponse<Void>>>();
		
		for(var file : files.split(","))
		{
			var url = Utils.encodeURL(String.format("http://%s%s", serverAddress, api.FileDelete.getEndpoint(String.format("%s,%s", directory, file))));
			var httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
			futures.add(ServerInteract.client.sendAsync(httpRequest, BodyHandlers.discarding()));
		}
		
		try
		{
			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
		}
		catch(Exception e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
		}
		
		response.sendRedirect(redirectURL);
	}
}
