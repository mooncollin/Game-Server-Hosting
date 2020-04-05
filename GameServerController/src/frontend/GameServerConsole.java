package frontend;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import api.minecraft.MinecraftServerCommandHandler;
import backend.api.ServerInteract;
import backend.main.StartUpApplication;
import frontend.templates.Templates.TriggerInfo;
import model.Query;
import model.Table;
import models.GameServerTable;
import models.TriggersTable;
import nodeapi.ApiSettings;
import server.GameServerCommandHandler;
import server.TriggerHandler;
import utils.ParameterURL;

@WebServlet("/GameServerConsole")
public class GameServerConsole extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameServerConsole";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL getEndpoint(int id)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID.getName(), id);
		return url;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		
		if(serverID.isEmpty())
		{
			response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
			return;
		}
		
		var serverType = StartUpApplication.serverTypes.get(serverID.get());
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID.get());
		
		if(serverType == null)
		{
			response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
			return;
		}
		
		Table gameserver;
		var triggers = new LinkedList<TriggerInfo>();
		
		try
		{
			var serverOptional = Query.query(StartUpApplication.database, GameServerTable.class)
									  .filter(GameServerTable.ID.cloneWithValue(serverID.get()))
									  .first();
			
			if(serverOptional.isEmpty())
			{
				response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
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
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
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
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("randomBackground", Assets.getRandomMinecraftBackground());
		context.put("serverName", gameserver.getColumnValue(GameServerTable.NAME));
		context.put("serverID", serverID.get());
		context.put("socketAddress", outputEndpoint.getURL());
		context.put("serverCommandRequest", ServerInteract.getEndpoint(serverID.get(), MinecraftServerCommandHandler.SERVER_COMMAND_COMMAND).getURL());
		context.put("serverStartRequest", ServerInteract.getEndpoint(serverID.get(), GameServerCommandHandler.START_COMMAND).getURL());
		context.put("serverStopRequest", ServerInteract.getEndpoint(serverID.get(), GameServerCommandHandler.STOP_COMMAND).getURL());
		context.put("serverLastRequest", ServerInteract.getEndpoint(serverID.get(), MinecraftServerCommandHandler.LAST_COMMAND).getURL());
		context.put("serverCommandEnd", String.format("&%s=", MinecraftServerCommandHandler.SERVER_COMMAND_COMMAND));
		context.put("triggerTypes", new String[] {TriggerHandler.RECURRING_TYPE, TriggerHandler.TIME_TYPE, TriggerHandler.OUTPUT_TYPE});
		context.put("actionTypes", new String[] {TriggerHandler.START_SERVER, TriggerHandler.STOP_SERVER, TriggerHandler.RESTART_SERVER});
		context.put("triggerOldValues", triggerOldValues);
		context.put("triggers", triggers);
		
		var template = Velocity.getTemplate("gameconsole.vm");
		template.merge(context, response.getWriter());
	}
}
