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
import java.util.regex.Matcher;
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
import model.TableTemp;
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
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverName = request.getParameter("name");
		var value = request.getParameter("value");
		var idStr = request.getParameter("id");
		var command = Objects.requireNonNullElse(request.getParameter("command"), "");
		var action = Objects.requireNonNullElse(request.getParameter("action"), "");
		var type = request.getParameter("type");
		int id;
		
		final String redirectUrl = Utils.encodeURL(String.format("%s?name=%s", GameServerConsole.URL, serverName));
		
		if(serverName == null || value == null || idStr == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		if(type != null && type.isBlank() || value.isBlank())
		{
			response.sendRedirect(redirectUrl);
			return;
		}
		
		try
		{
			id = Integer.valueOf(idStr);
		}
		catch(NumberFormatException e)
		{
			response.sendRedirect(redirectUrl);
			return;
		}
		
		var foundServer = StartUpApplication.getServerInfo().get(serverName);
		if(foundServer == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		TableTemp trigger;
		try
		{
			trigger = Query.query(StartUpApplication.database, TriggersTable.class)
							   .filter(TriggersTable.ID.cloneWithValue(id))
							   .first();
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
		
		if(trigger == null)
		{
			trigger = new TriggersTable();
			trigger.setColumnValue(TriggersTable.TYPE, type.toLowerCase());
			trigger.setColumnValue(TriggersTable.COMMAND, "");
			trigger.setColumnValue(TriggersTable.VALUE, "");
			trigger.setColumnValue(TriggersTable.SERVER_OWNER, serverName);
			trigger.setColumnValue(TriggersTable.EXTRA, "");
		}
		
		var parsedValue = value;
		var triggerValue = trigger.getColumnValue(TriggersTable.TYPE);
		
		if(triggerValue.equals("recurring"))
		{
			Matcher hourMatcher = TIME_HOUR.matcher(value);
			Matcher minuteMatcher = TIME_MINUTE.matcher(value);
			Matcher secondMatcher = TIME_SECOND.matcher(value);
			String hour = hourMatcher.matches() ? hourMatcher.group("hour") + "H" : "";
			String minute = minuteMatcher.matches() ? minuteMatcher.group("minute") + "M" : "";
			String second = secondMatcher.matches() ? secondMatcher.group("second") + "S" : "";
			
			if(hour.isEmpty() && minute.isEmpty() && second.isEmpty())
			{
				response.sendRedirect(redirectUrl);
				return;
			}
			
			try
			{
				Duration dur = Duration.parse(String.format("PT%s%s%s", hour, minute, second));
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
				LocalTime t = LocalTime.parse(value);
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
		
		
		final String url = Utils.encodeURL(String.format("http://%s/TriggerEdit?id=%s", foundServer.getSecond(), trigger.getColumnValue(TriggersTable.ID)));
		
		try
		{
			HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
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
