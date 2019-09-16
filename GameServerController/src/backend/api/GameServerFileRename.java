package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

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
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
		String directory = request.getParameter("directory");
		String rename = request.getParameter("rename");
		String newFolder = request.getParameter("newFolder");
		if(serverName == null || directory == null || (rename == null && newFolder == null))
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
		
		int lastIndex = directory.lastIndexOf(',');
		if(lastIndex == -1 || newFolder != null)
		{
			lastIndex = directory.length();
		}
		
		final String redirectURL = Utils.encodeURL(GameServerFiles.URL + "?name=" + serverName + "&directory=" + directory.substring(0, lastIndex));
		
		try
		{
			String url = "http://" + serverFound.getSecond() + "/FileRename?directory=" + directory;
			if(rename != null)
			{
				url += "&rename=" + rename.replace(' ', '+');
			}
			else
			{
				url += "&newFolder=" + newFolder.replace(' ', '+');
			}
			HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(Utils.encodeURL(url))).build();
			HttpResponse<String> httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.ofString());
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
