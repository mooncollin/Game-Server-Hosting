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
import server.TriggerHandler;
import utils.Utils;

@WebServlet("/GameServerTriggerEdit")
public class GameServerTriggerEdit extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final Pattern RECURRING_PATTERN = Pattern.compile("(?<hour>[01]?[0-9]|2[0-3]):(?<minute>[0-5][0-9]):(?<second>[0-5][0-9])");
	
	public static final String URL = "/GameServerController/GameServerTriggerEdit";
	
	public static String getEndpoint(int serverID, int triggerID)
	{
		return String.format("%s?id=%d&triggerID=%d", URL, serverID, triggerID);
	}
	
	public static String getEndpoint(int serverID, int triggerID, String value, String command, String action, String type)
	{
		
		return String.format("%s?id=%d&triggerID=%d&value=%s&type=%s&command=%s&action=%s", 
			URL, serverID, triggerID, value, type, command, action);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter("id"));
		var triggerID = Utils.fromString(Integer.class, request.getParameter("triggerID"));
		var value = request.getParameter("value");
		var command = Objects.requireNonNullElse(request.getParameter("command"), "");
		var action = Objects.requireNonNullElse(request.getParameter("action"), "");
		var type = request.getParameter("type");
		
		if(serverID == null || value == null || triggerID == null || (triggerID == -1 && type == null))
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var redirectUrl = GameServerConsole.getEndpoint(serverID);
		
		var serverAddress = StartUpApplication.serverAddresses.get(serverID);
		
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
				response.sendRedirect(redirectUrl);
				return;
			}
			
			var hour = Objects.requireNonNullElse(Utils.fromString(Integer.class, matcher.group("hour")), 0);
			var minute = Objects.requireNonNullElse(Utils.fromString(Integer.class, matcher.group("minute")), 0);
			var second = Objects.requireNonNullElse(Utils.fromString(Integer.class, matcher.group("second")), 0);
			
			if(hour == 0 && minute == 0 && second == 0)
			{
				response.sendRedirect(redirectUrl);
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
				response.sendRedirect(redirectUrl);
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
			response.sendRedirect(redirectUrl);
			return;
		}
		
		
		var url = String.format("http://%s%s", serverAddress, api.TriggerEdit.getEndpoint(trigger.getColumnValue(TriggersTable.ID)));
		
		try
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
			ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
		}
		catch(InterruptedException e)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		response.sendRedirect(redirectUrl);
	}
}
