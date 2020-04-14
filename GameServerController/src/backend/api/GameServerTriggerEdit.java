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
import model.Query;
import model.Table;
import models.TriggersTable;
import nodeapi.ApiSettings;
import server.TriggerHandler;
import server.TriggerHandlerRecurring;
import server.TriggerHandlerTime;
import utils.Utils;

@WebServlet(
		name = "GameServerTriggerEdit",
		urlPatterns = "/GameServerTriggerEdit",
		asyncSupported = true
)
/**
 * Backend endpoint for editing a trigger. Responsible for editing the database entry
 * and relaying the new trigger information to the corresponding node so that it may 
 * change the running trigger.
 * @author Collin
 *
 */
public class GameServerTriggerEdit extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var triggerID = ApiSettings.TRIGGER_ID.parse(request);
		var value = ApiSettings.TRIGGER_VALUE.parse(request);
		var command = ApiSettings.TRIGGER_COMMAND.parse(request);
		var action = ApiSettings.TRIGGER_ACTION.parse(request);
		
		if(!Utils.optionalsPresent(serverID, triggerID, value, command, action))
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
		
		Table trigger;
		try
		{
			var option = Query.query(StartUpApplication.database, TriggersTable.class)
							   .filter(TriggersTable.ID, triggerID.get())
							   .first();
			
			if(option.isEmpty())
			{
				response.sendRedirect(redirectUrl.getURL());
				return;
			}

			trigger = option.get();
			
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.error(e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var parsedValue = value.get();
		var triggerType = trigger.getColumnValue(TriggersTable.TYPE);
		
		if(triggerType.equals(TriggerHandler.RECURRING_TYPE))
		{
			var seconds = TriggerHandlerRecurring.convertFormatToSeconds(value.get());
			if(seconds <= 0)
			{
				response.sendRedirect(redirectUrl.getURL());
				return;
			}

			parsedValue = String.valueOf(seconds);
		}
		else if(triggerType.equals(TriggerHandler.TIME_TYPE))
		{
			var seconds = TriggerHandlerTime.convertFormatToSeconds(value.get());
			if(seconds < 0)
			{
				response.sendRedirect(redirectUrl.getURL());
				return;
			}
			
			parsedValue = String.valueOf(seconds);
		}
		
		trigger.setColumnValue(TriggersTable.VALUE, parsedValue);
		trigger.setColumnValue(TriggersTable.COMMAND, command.get());
		trigger.setColumnValue(TriggersTable.EXTRA, action.get());
		
		try
		{
			trigger.commit(StartUpApplication.database);
		} catch (SQLException e)
		{
			response.sendRedirect(redirectUrl.getURL());
			return;
		}
		
		
		var url = nodeapi.TriggerEdit.getEndpoint(triggerID.get());
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
