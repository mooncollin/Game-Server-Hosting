package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.Endpoints;
import model.Query;
import model.Table;
import models.GameServerTable;
import nodeapi.ApiSettings;

@WebServlet(
		name = "GameServerDelete",
		urlPatterns = "/GameServerDelete",
		asyncSupported = true
)
/**
 * Backend endpoint for deleting a game server. Responsible for deleting
 * the database and notifying the node that the server has been deleted.
 * @author Collin
 *
 */
public class GameServerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		if(serverID.isEmpty())
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		var serverAddress = StartUpApplication.getServerIPAddress(serverID.get());
		if(serverAddress == null)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.ID, serverID.get())
								  .first();
			
			Table gameServer;
			
			if(option.isEmpty())
			{
				response.setStatus(400);
				return;
			}
			else
			{
				gameServer = option.get();
			}
			
			gameServer.delete(StartUpApplication.database);
		}
		catch(SQLException e)
		{
			StartUpApplication.LOGGER.error(String.format("Error deleting game server:\n%s", e.getMessage()));
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		StartUpApplication.removeServerIPAddress(serverID.get());
		
		var url = nodeapi.ServerDelete.postEndpoint(serverID.get());
		url.setHost(serverAddress);
		
		try
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL()))
										 .POST(BodyPublishers.noBody())
										 .build();
			
			StartUpApplication.client.send(httpRequest, BodyHandlers.discarding());
		}
		catch(InterruptedException e)
		{
			
		}
		
		response.sendRedirect(Endpoints.INDEX.get().getURL());
	}
}
