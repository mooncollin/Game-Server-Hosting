package backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.GameServerConsole;
import frontend.Index;
import model.Query;
import model.Table;
import models.GameServerTable;
import models.TriggersTable;
import nodeapi.ApiSettings;
import server.TriggerHandler;
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/GameServerTriggerEdit")
public class GameServerTriggerEdit extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final Pattern RECURRING_PATTERN = Pattern.compile("(?<hour>[01]?[0-9]|2[0-3]):(?<minute>[0-5][0-9]):(?<second>[0-5][0-9])");
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameServerTriggerEdit";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL postEndpoint(int serverID, int triggerID)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID_PARAMETER, serverID);
		url.addQuery(ApiSettings.TRIGGER_ID_PARAMETER, triggerID);
		return url;
	}
	
	public static ParameterURL postEndpoint(int serverID, int triggerID, String value, String command, String action, String type)
	{
		var url = postEndpoint(serverID, triggerID);
		url.addQuery(ApiSettings.TRIGGER_VALUE_PARAMETER, value);
		url.addQuery(ApiSettings.TRIGGER_TYPE_PARAMETER, type);
		url.addQuery(ApiSettings.TRIGGER_COMMAND_PARAMETER, command);
		url.addQuery(ApiSettings.TRIGGER_ACTION_PARAMETER, action);
		return url;
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		var triggerID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.TRIGGER_ID_PARAMETER));
		var value = request.getParameter(ApiSettings.TRIGGER_VALUE_PARAMETER);
		var command = Objects.requireNonNullElse(request.getParameter(ApiSettings.TRIGGER_COMMAND_PARAMETER), "");
		var action = Objects.requireNonNullElse(request.getParameter(ApiSettings.TRIGGER_ACTION_PARAMETER), "");
		var type = request.getParameter(ApiSettings.TRIGGER_TYPE_PARAMETER);
		
		if(serverID == null || value == null || triggerID == null || (triggerID == -1 && type == null))
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var redirectUrl = GameServerConsole.getEndpoint(serverID);
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID);
		
		if(serverAddress == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		Table trigger;
		try
		{
			var option = Query.query(StartUpApplication.database, TriggersTable.class)
							   .filter(TriggersTable.ID, triggerID)
							   .first();
			
			if(option.isEmpty())
			{
				var gameServer = Query.query(StartUpApplication.database, GameServerTable.class)
									  .filter(GameServerTable.ID, serverID)
									  .first();
				
				if(gameServer.isEmpty()) // Just checking if the server actually exists
				{
					response.sendRedirect(Index.URL);
					return;
				}
				
				trigger = new TriggersTable();
				trigger.setColumnValue(TriggersTable.TYPE, type.toLowerCase());
				trigger.setColumnValue(TriggersTable.COMMAND, "");
				trigger.setColumnValue(TriggersTable.VALUE, "");
				trigger.setColumnValue(TriggersTable.SERVER_OWNER, serverID);
				trigger.setColumnValue(TriggersTable.EXTRA, "");
			}
			else
			{
				trigger = option.get();
			}
			
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var parsedValue = value;
		var triggerType = trigger.getColumnValue(TriggersTable.TYPE);
		
		if(triggerType.equals(TriggerHandler.RECURRING_TYPE))
		{	
			var matcher = RECURRING_PATTERN.matcher(value);
			if(!matcher.matches())
			{
				response.sendRedirect(redirectUrl.getURL());
				return;
			}
			
			var hour = Objects.requireNonNullElse(Utils.fromString(Integer.class, matcher.group("hour")), 0);
			var minute = Objects.requireNonNullElse(Utils.fromString(Integer.class, matcher.group("minute")), 0);
			var second = Objects.requireNonNullElse(Utils.fromString(Integer.class, matcher.group("second")), 0);
			
			if(hour == 0 && minute == 0 && second == 0)
			{
				response.sendRedirect(redirectUrl.getURL());
				return;
			}
			
			var dur = Duration.ZERO.plusHours(hour).plusMinutes(minute).plusSeconds(second);
			parsedValue = String.valueOf(dur.getSeconds());
		}
		else if(triggerType.equals(TriggerHandler.TIME_TYPE))
		{
			try
			{
				var t = LocalTime.parse(value);
				parsedValue = String.valueOf(t.toSecondOfDay());
			}
			catch(DateTimeParseException e)
			{
				response.sendRedirect(redirectUrl.getURL());
				return;
			}
		}
		
		trigger.setColumnValue(TriggersTable.VALUE, parsedValue);
		trigger.setColumnValue(TriggersTable.COMMAND, command);
		trigger.setColumnValue(TriggersTable.EXTRA, action);
		
		try
		{
			trigger.commit(StartUpApplication.database);
		} catch (SQLException e)
		{
			response.sendRedirect(redirectUrl.getURL());
			return;
		}
		
		
		var url = nodeapi.TriggerEdit.getEndpoint(trigger.getColumnValue(TriggersTable.ID));
		url.setHost(serverAddress);
		
		try
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
		}
		catch(InterruptedException e)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		response.sendRedirect(redirectUrl.getURL());
	}
}
