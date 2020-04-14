package minecraft.frontend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import backend.main.StartUpApplication;
import frontend.Endpoints;
import minecraft.server.MinecraftServer;
import minecraft.server.MinecraftServerCommandHandler;
import minecraft.server.MinecraftServerTable;
import minecraft.server.MinecraftServerUIHandler;
import model.Query;
import model.Table;
import models.GameServerTable;
import module.MinecraftGameServerModule;
import nodeapi.ApiSettings;
import utils.Utils;

public class Settings extends HttpServlet
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
		
		Table gameServerTable;
		Table minecraftTable;
		try
		{
			var option = Query.query(StartUpApplication.database, MinecraftServerTable.class)
							  .filter(MinecraftServerTable.ID, serverID.get())
							  .first();
			
			var option2 = Query.query(StartUpApplication.database, GameServerTable.class)
							   .filter(GameServerTable.ID, serverID.get())
							   .first();
			
			if(option.isEmpty() && option2.isEmpty())
			{
				response.sendRedirect(Endpoints.INDEX.get().getURL());
				return;
			}
			
			minecraftTable = option.get();
			gameServerTable = option2.get();
		}
		catch(SQLException e)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		JSONObject nodeResponse;
		try
		{
			var url = nodeapi.ServerInteract.postEndpoint(serverID.get());
			url.setHost(serverAddress);
			
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL()))
										 .POST(BodyPublishers.ofString(new JSONObject(Map.of("command", MinecraftServerCommandHandler.GET_PROPERTIES_COMMAND_NAME)).toJSONString()))
										 .build();
			
			var httpResponse = StartUpApplication.client.send(httpRequest, BodyHandlers.ofString());
			nodeResponse = (JSONObject) JSONValue.parseWithException(httpResponse.body());
		}
		catch(InterruptedException e)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		catch(ParseException e)
		{
			MinecraftGameServerModule.LOGGER.error(String.format("Error parsing response from node:\n%s", e.getMessage()));
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		@SuppressWarnings("unchecked")
		var defaultProperties = new TreeMap<String, Object>((JSONObject) nodeResponse.get("result"));
		
		var properties = new LinkedList<PropertyInfo>();
		for(var entry : defaultProperties.entrySet())
		{
			properties.add(new PropertyInfo(entry.getKey(), minecraftPropertyToInputType(MinecraftServer.MINECRAFT_PROPERTIES.get(entry.getKey())), entry.getValue().toString()));
		}
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("serverID", serverID.get());
		context.put("serverName", gameServerTable.getColumnValue(GameServerTable.NAME));
		context.put("module", new MinecraftGameServerModule());
		context.put("minimumRamAmount", MinecraftServer.MINIMUM_HEAP_SIZE);
		context.put("ramStep", MinecraftServer.HEAP_STEP);
		context.put("ramAmount", minecraftTable.getColumnValue(MinecraftServerTable.MAX_HEAP_SIZE));
		context.put("properties", properties);
		
		var template = Velocity.getTemplate("minecraft/settings.vm");
		template.merge(context, response.getWriter());
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var ramAmount = MinecraftServerUIHandler.RAM_AMOUNT.parse(request);
		
		if(!Utils.optionalsPresent(serverID, ramAmount))
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
		
		Table minecraftTable;
		try
		{
			var option = Query.query(StartUpApplication.database, MinecraftServerTable.class)
							  .filter(MinecraftServerTable.ID, serverID.get())
							  .first();
			
			if(option.isEmpty())
			{
				response.sendRedirect(Endpoints.INDEX.get().getURL());
				return;
			}
			
			minecraftTable = option.get();
			
			minecraftTable.setColumnValue(MinecraftServerTable.MAX_HEAP_SIZE, ramAmount.get());
			minecraftTable.commit(StartUpApplication.database);
		}
		catch(SQLException e)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		var properties = new JSONObject();
		properties.put("value", new JSONObject(MinecraftServer.MINECRAFT_PROPERTIES));
		for(var key : MinecraftServer.MINECRAFT_PROPERTIES.keySet())
		{
			var newProperty = request.getParameter((String) key);
			if(newProperty != null)
			{
				if(MinecraftServer.MINECRAFT_PROPERTIES.get(key) instanceof Boolean)
				{
					((Map) properties.get("value")).put(key, newProperty.equals("on"));
				}
				else
				{
					((Map) properties.get("value")).put(key, newProperty);
				}
			}
		}
		properties.put("command", MinecraftServerCommandHandler.SET_PROPERTIES_COMMAND_NAME);
		
		try
		{
			var url = nodeapi.ServerInteract.postEndpoint(serverID.get());
			url.setHost(serverAddress);
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL()))
					.header("Content-Type", "application/json")
					.POST(BodyPublishers.ofString(properties.toJSONString()))
					.build();
			StartUpApplication.client.send(httpRequest, BodyHandlers.discarding());
		}
		catch(InterruptedException e)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		doGet(request, response);
	}
	
	public static String minecraftPropertyToInputType(Object prop)
	{
		if(prop instanceof Integer)
		{
			return "number";
		}
		else if(prop instanceof Boolean)
		{
			return "checkbox";
		}
		
		return "text";
	}
	
	public static class PropertyInfo
	{
		private String name;
		private String type;
		private String value;
		
		public PropertyInfo(String name, String type, String value)
		{
			this.name = name;
			this.type = type;
			this.value = value;
		}
		
		public String getName()
		{
			return name;
		}
		
		public String getType()
		{
			return type;
		}
		
		public String getValue()
		{
			return value;
		}
	}
}
