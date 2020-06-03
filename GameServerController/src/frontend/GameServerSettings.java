package frontend;

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

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import backend.main.StartUpApplication;
import model.Query;
import model.Table;
import models.GameServerTable;
import nodeapi.ApiSettings;
import utils.Utils;

/**
 * The frontend for a game server's settings.
 * @author Collin
 *
 */
@WebServlet(
		name = "GameServerSettings",
		urlPatterns = "/GameServerSettings",
		asyncSupported = true
)
public class GameServerSettings extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		if(!Utils.optionalsPresent(serverID))
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
		
		Table foundGameServer;
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
					   			   .filter(GameServerTable.ID, serverID.get())
					   			   .first();
			if(option.isEmpty())
			{
				response.sendRedirect(Endpoints.INDEX.get().getURL());
				return;
			}
			
			foundGameServer = option.get();
		} catch (SQLException e)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("serverID", serverID.get());
		context.put("serverName", foundGameServer.getColumnValue(GameServerTable.NAME));
		context.put("executableName", foundGameServer.getColumnValue(GameServerTable.EXECUTABLE_NAME));
		context.put("arguments", foundGameServer.getColumnValue(GameServerTable.ARGUMENTS));
		context.put("module", StartUpApplication.getModule(foundGameServer.getColumnValue(GameServerTable.SERVER_TYPE)));
		context.put("restarts", foundGameServer.getColumnValue(GameServerTable.AUTO_RESTARTS));
		
		var template = Velocity.getTemplate("settings.vm");
		template.merge(context, response.getWriter());
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var execName = ApiSettings.EXECUTABLE_NAME.parse(request);
		var arguments = ApiSettings.ARGUMENTS.parse(request);
		var restarts = ApiSettings.RESTARTS_UNEXPECTED.parse(request);
		if(!Utils.optionalsPresent(serverID, execName, arguments, restarts))
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
		
		var redirectURL = Endpoints.GAME_SERVER_SETTINGS.get(serverID.get());
		
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
							  .filter(GameServerTable.ID, serverID.get())
							  .first();
			
			if(option.isEmpty())
			{
				StartUpApplication.LOGGER.error("Server exists in cache, but not in the database!");
				response.sendRedirect(Endpoints.INDEX.get().getURL());
				return;
			}
			
			var gameServer = option.get();
			
			gameServer.setColumnValue(GameServerTable.EXECUTABLE_NAME, execName.get());
			gameServer.setColumnValue(GameServerTable.ARGUMENTS, arguments.get());
			gameServer.setColumnValue(GameServerTable.AUTO_RESTARTS, restarts.get());
			
			gameServer.commit(StartUpApplication.database);
		}
		catch(SQLException e)
		{
			StartUpApplication.LOGGER.error("Failure to query/update database in [GameServerSettings]");
			response.sendRedirect(redirectURL.getURL());
			return;
		}
		
		var sendURL = nodeapi.Endpoints.SERVER_EDIT.post(serverID.get());
		sendURL.setHost(serverAddress);
		
		try
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(sendURL.getURL()))
										 .POST(BodyPublishers.noBody())
										 .build();
			
			StartUpApplication.client.send(httpRequest, BodyHandlers.discarding());
		}
		catch(InterruptedException e)
		{
			response.sendRedirect(redirectURL.getURL());
			return;
		}
		
		doGet(request, response);
	}
}
