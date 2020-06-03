package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.Endpoints;
import models.TriggersTable;
import nodeapi.ApiSettings;
import server.TriggerHandler;
import server.TriggerHandlerRecurring;
import server.TriggerHandlerTime;
import utils.Utils;

@WebServlet(
		name = "GameServerTriggerAdd",
		urlPatterns = "/GameServerTriggerAdd",
		asyncSupported = true
)
/**
 * Backend endpoint for adding a trigger to a specific game server.
 * Responsible for adding the trigger to the database and relaying
 * a new trigger to the corresponding node.
 * @author Collin
 *
 */
public class GameServerTriggerAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var value = ApiSettings.TRIGGER_VALUE.parse(request);
		var command = ApiSettings.TRIGGER_COMMAND.parse(request);
		var action = ApiSettings.TRIGGER_ACTION.parse(request);
		var type = ApiSettings.TRIGGER_TYPE.parse(request);
		
		if(!Utils.optionalsPresent(serverID, value, command, action, type))
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
		
		var redirectUrl = Endpoints.GAME_SERVER_CONSOLE.get(serverID.get());
		
		var parsedValue = value.get();
		
		if(type.get().equals(TriggerHandler.RECURRING_TYPE))
		{
			var seconds = TriggerHandlerRecurring.convertFormatToSeconds(value.get());
			if(seconds <= 0)
			{
				response.sendRedirect(redirectUrl.getURL());
				return;
			}

			parsedValue = String.valueOf(seconds);
		}
		else if(type.get().equals(TriggerHandler.TIME_TYPE))
		{
			var seconds = TriggerHandlerTime.convertFormatToSeconds(value.get());
			if(seconds < 0)
			{
				response.sendRedirect(redirectUrl.getURL());
				return;
			}
			
			parsedValue = String.valueOf(seconds);
		}
		
		var trigger = new TriggersTable();
		trigger.setColumnValue(TriggersTable.SERVER_OWNER, serverID.get());
		trigger.setColumnValue(TriggersTable.TYPE, type.get());
		trigger.setColumnValue(TriggersTable.VALUE, parsedValue);
		trigger.setColumnValue(TriggersTable.COMMAND, command.get());
		trigger.setColumnValue(TriggersTable.EXTRA, action.get());
		
		try
		{
			trigger.commit(StartUpApplication.database);
		}
		catch(SQLException e)
		{
			StartUpApplication.LOGGER.error("Unable to create new trigger in the database");
			response.sendRedirect(redirectUrl.getURL());
			return;
		}
		
		var url = nodeapi.Endpoints.TRIGGER_EDIT.get(trigger.getColumnValue(TriggersTable.ID));
		url.setHost(serverAddress);
		
		try
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			StartUpApplication.client.send(httpRequest, BodyHandlers.discarding());
		}
		catch(InterruptedException e)
		{
		}
		
		response.sendRedirect(redirectUrl.getURL());
	}
}
