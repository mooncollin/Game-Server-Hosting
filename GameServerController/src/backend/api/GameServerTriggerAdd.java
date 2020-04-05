package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.GameServerConsole;
import frontend.Index;
import models.TriggersTable;
import nodeapi.ApiSettings;
import server.TriggerHandler;
import server.TriggerHandlerRecurring;
import server.TriggerHandlerTime;
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/GameServerTriggerAdd")
public class GameServerTriggerAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameServerTriggerAdd";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL postEndpoint(int serverID, String value, String command, String action, String type)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
		url.addQuery(ApiSettings.TRIGGER_VALUE.getName(), value);
		url.addQuery(ApiSettings.TRIGGER_TYPE.getName(), type);
		url.addQuery(ApiSettings.TRIGGER_COMMAND.getName(), command);
		url.addQuery(ApiSettings.TRIGGER_ACTION.getName(), action);
		return url;
	}
	
	public static ParameterURL postEndpoint(int serverID)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
		return url;
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var value = ApiSettings.TRIGGER_VALUE.parse(request);
		var command = ApiSettings.TRIGGER_COMMAND.parse(request);
		var action = ApiSettings.TRIGGER_ACTION.parse(request);
		var type = ApiSettings.TRIGGER_TYPE.parse(request);
		
		if(!Utils.optionalsPresent(serverID, value, command, action, type))
		{
			response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
			return;
		}
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID.get());
		
		if(serverAddress == null)
		{
			response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
			return;
		}
		
		var redirectUrl = GameServerConsole.getEndpoint(serverID.get());
		
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
			StartUpApplication.LOGGER.log(Level.SEVERE, "Unable to create new trigger in the database");
			response.sendRedirect(redirectUrl.getURL());
			return;
		}
		
		var url = nodeapi.TriggerEdit.getEndpoint(trigger.getColumnValue(TriggersTable.ID));
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
