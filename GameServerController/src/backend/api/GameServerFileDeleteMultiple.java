package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
		String directory = request.getParameter("directory");
		String files = request.getParameter("files");
		if(serverName == null || directory == null || files == null)
		{
			response.setStatus(400);
			return;
		}
		
		var serverFound = StartUpApplication.getServerInfo().get(serverName);
		if(serverFound == null || directory.isEmpty())
		{
			response.setStatus(404);
			return;
		}
		
		for(String d : directory.split(","))
		{
			if(d.contains(".."))
			{
				response.setStatus(400);
				return;
			}
		}
		
		
		
		final String redirectURL = Utils.encodeURL(GameServerFiles.URL + "?name=" + serverName + "&directory=" + directory);
		
		List<CompletableFuture<HttpResponse<Void>>> futures = new LinkedList<CompletableFuture<HttpResponse<Void>>>();
		
		for(String file : files.split(","))
		{
			final String url = Utils.encodeURL(String.format("http://%s/FileDelete?directory=%s,%s", serverFound.getSecond(), directory, file));
			HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
			futures.add(ServerInteract.client.sendAsync(httpRequest, BodyHandlers.discarding()));
		}
		
		try
		{
			CompletableFuture.allOf(futures.toArray(new CompletableFuture[] {}));
		}
		catch(Exception e)
		{
			
		}
		
		response.sendRedirect(redirectURL);
	}
}
