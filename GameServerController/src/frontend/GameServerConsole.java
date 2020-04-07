package frontend;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import backend.main.StartUpApplication;
import frontend.templates.Templates;
import frontend.templates.Templates.TriggerInfo;
import model.Query;
import model.Table;
import models.GameServerTable;
import models.TriggersTable;
import nodeapi.ApiSettings;
import server.TriggerHandler;

@WebServlet(
		name = "GameServerConsole",
		urlPatterns = "/GameServerConsole",
		asyncSupported = true
)
@ServletSecurity(
		httpMethodConstraints = @HttpMethodConstraint(value = "GET")
)
public class GameServerConsole extends HttpServlet
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
		
		Table gameserver;
		var triggers = new LinkedList<TriggerInfo>();
		
		try
		{
			var serverOptional = Query.query(StartUpApplication.database, GameServerTable.class)
									  .filter(GameServerTable.ID.cloneWithValue(serverID.get()))
									  .first();
			
			if(serverOptional.isEmpty())
			{
				response.sendRedirect(Endpoints.INDEX.get().getURL());
				return;
			}
			
			gameserver = serverOptional.get();
			
			var triggerQuery = Query.query(StartUpApplication.database, TriggersTable.class)
								.filter(TriggersTable.SERVER_OWNER, serverID.get())
								.all();

			for(var row : triggerQuery)
			{
				triggers.add(new TriggerInfo(row));
			}
			
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.error(e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var outputEndpoint = nodeapi.Output.getEndpoint(serverID.get());
		outputEndpoint.setHost(serverAddress);
		
		var triggerOldValues = new HashMap<Integer, Map<String, String>>();
		
		for(var trigger : triggers)
		{
			triggerOldValues.put(trigger.getId(), Map.ofEntries(
				Map.entry("value", trigger.getValue()),
				Map.entry("command", trigger.getCommand()),
				Map.entry("action", trigger.getExtra())
			));
		}
		
		var outputAddresses = StartUpApplication.getNodeOutputAddresses("");
		outputAddresses.entrySet().removeIf(e -> !e.getKey().equals(serverID.get()));
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("randomBackground", Assets.getRandomMinecraftBackground());
		context.put("serverName", gameserver.getColumnValue(GameServerTable.NAME));
		context.put("serverID", serverID.get());
		context.put("serverCommandEndpoint", Templates.getServerCommandEndpoint());
		context.put("nodeOutputAddresses", outputAddresses);
		context.put("triggerTypes", List.of(TriggerHandler.RECURRING_TYPE, TriggerHandler.TIME_TYPE, TriggerHandler.OUTPUT_TYPE));
		context.put("actionTypes", List.of(TriggerHandler.START_SERVER, TriggerHandler.STOP_SERVER, TriggerHandler.RESTART_SERVER));
		context.put("triggerOldValues", triggerOldValues);
		context.put("triggers", triggers);
		
		var template = Velocity.getTemplate("gameconsole.vm");
		template.merge(context, response.getWriter());
	}
}
