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
import utils.Utils;

@WebServlet("/GameServerFile")
public class GameServerFileDownload extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	public static final String URL = "/GameServerController/GameServerFile";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
		String directory = request.getParameter("directory");
		if(serverName == null || directory == null)
		{
			response.setStatus(400);
			return;
		}
		
		var serverFound = StartUpApplication.getServerInfo().get(serverName);
		if(serverFound == null)
		{
			response.setStatus(404);
			return;
		}
		
		if(directory.isEmpty())
		{
			directory = serverName;
		}
		
		String[] directories = directory.split(",");
		
		for(String d : directories)
		{
			if(d.contains(".."))
			{
				response.setStatus(400);
				return;
			}
		}
		
		byte[] data;
		
		try
		{
			final String url = Utils.encodeURL("http://" + serverFound.getSecond() + "/FileDownload?directory=" + directory);
			HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
			HttpResponse<byte[]> httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.ofByteArray());
			if(httpResponse.statusCode() != 200)
			{
				response.setStatus(400);
				return;
			}
			
			data = httpResponse.body();
		}
		catch(InterruptedException e)
		{
			response.setStatus(500);
			return;
		}
		
		String fileName = directories[directories.length - 1];
		if(fileName.indexOf('.') == -1)
		{
			fileName += ".zip";
		}

		response.setHeader("Content-disposition","attachment; filename=\"" + fileName + "\"");
		response.getOutputStream().write(data);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
	}
}
