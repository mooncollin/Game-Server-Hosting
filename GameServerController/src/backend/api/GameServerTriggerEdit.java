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
import utils.Utils;

@WebServlet("/GameServerTriggerEdit")
public class GameServerTriggerEdit extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final Pattern TIME_HOUR = Pattern.compile(".*?(?<hour>(?:[0-2])?[0-9])(?:h|H).*");
	public static final Pattern TIME_MINUTE = Pattern.compile(".*?(?<minute>(?:[0-5])?[0-9])(?:m|M).*");
	public static final Pattern TIME_SECOND = Pattern.compile(".*?(?<second>(?:[0-5])?[0-9])(?:s|S).*");
	
	public static final String URL = "/GameServerController/GameServerTriggerEdit";
	
	public static String getEndpoint(int serverID, int triggerID, String value, String command, String action, String type)
	{
		var commandString = "";
		var actionString = "";
		if(command != null && !command.isEmpty())
		{
			commandString = String.format("&command=%s", command);
		}
		
		if(action != null && !action.isEmpty())
		{
			actionString = String.format("&action=%s", action);
		}
		
		return String.format("%s?id=%d&triggerID=%d&value=%s&type=%s%s%s", 
			URL, serverID, triggerID, value, type, commandString, actionString);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverIDStr = request.getParameter("id");
		var value = request.getParameter("value");
		var triggerIDStr = request.getParameter("triggerID");
		var command = Objects.requireNonNullElse(request.getParameter("command"), "");
		var action = Objects.requireNonNullElse(request.getParameter("action"), "");
		var type = request.getParameter("type");
		
		
		if(serverIDStr == null || value == null || triggerIDStr == null || type == null || type.isBlank() || value.isBlank())
		{
			response.sendRedirect(Index.URL);
			return;
		}
		

		int triggerID;
		int serverID;
		try
		{
			triggerID = Integer.parseInt(triggerIDStr);
			serverID = Integer.parseInt(serverIDStr);
		}
		catch(NumberFormatException e)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var redirectUrl = Utils.encodeURL(GameServerConsole.getEndpoint(serverID));
		
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
		var triggerValue = trigger.getColumnValue(TriggersTable.TYPE);
		
		if(triggerValue.equals("recurring"))
		{
			var hourMatcher = TIME_HOUR.matcher(value);
			var minuteMatcher = TIME_MINUTE.matcher(value);
			var secondMatcher = TIME_SECOND.matcher(value);
			var hour = hourMatcher.matches() ? hourMatcher.group("hour") + "H" : "";
			var minute = minuteMatcher.matches() ? minuteMatcher.group("minute") + "M" : "";
			var second = secondMatcher.matches() ? secondMatcher.group("second") + "S" : "";
			
			if(hour.isEmpty() && minute.isEmpty() && second.isEmpty())
			{
				response.sendRedirect(redirectUrl);
				return;
			}
			
			try
			{
				var dur = Duration.parse(String.format("PT%s%s%s", hour, minute, second));
				parsedValue = String.valueOf(dur.getSeconds());
			}
			catch(DateTimeParseException e)
			{
				response.sendRedirect(redirectUrl);
				return;
			}
		}
		else if(triggerValue.equals("time"))
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
		
		
		var url = Utils.encodeURL(String.format("http://%s%s", serverAddress, trigger.getColumnValue(TriggersTable.ID)));
		
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
