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

import frontend.Index;
import utils.Utils;
import backend.main.StartUpApplication;

@WebServlet("/GameServerDelete")
public class GameServerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerDelete";

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String name = request.getParameter("name");
		if(name == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var serverFound = StartUpApplication.getServerInfo().get(name);
		if(serverFound == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		final String url = Utils.encodeURL(String.format("http://%s/ServerDelete?name=%s", serverFound.getSecond(), name));
		
		try
		{
			HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
			HttpResponse<Void> httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
			if(httpResponse.statusCode() == 200)
			{
				StartUpApplication.getServerInfo().remove(name);
			}
		}
		catch(InterruptedException e)
		{
			
		}
		
		response.sendRedirect(Index.URL);
	}
}
