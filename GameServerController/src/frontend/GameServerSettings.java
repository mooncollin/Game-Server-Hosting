package frontend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import api.minecraft.MinecraftServer;
import backend.main.StartUpApplication;
import frontend.templates.Templates;
import frontend.templates.Templates.PropertyInfo;
import model.Query;
import model.Table;
import models.GameServerTable;
import models.MinecraftServerTable;
import models.NodeTable;
import nodeapi.ApiSettings;
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/GameServerSettings")
public class GameServerSettings extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameServerSettings";
	
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
	
	public static ParameterURL postEndpoint(int id)
	{
		return getEndpoint(id);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		if(!Utils.optionalsPresent(serverID))
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
		
		Table foundGameServer;
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
					   			   .filter(GameServerTable.ID.cloneWithValue(serverID.get()))
					   			   .first();
			if(option.isEmpty())
			{
				response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
				return;
			}
			else
			{
				foundGameServer = option.get();
			}
		} catch (SQLException e)
		{
			response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
			return;
		}
		
		Table minecraftServerFound;
		try
		{
			var option = Query.query(StartUpApplication.database, MinecraftServerTable.class)
										.filter(MinecraftServerTable.SERVER_ID, foundGameServer.getColumnValue(GameServerTable.ID))
										.first();
			if(option.isEmpty())
			{
				response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
				return;
			}
			else
			{
				minecraftServerFound = option.get();
			}
		} catch (SQLException e2)
		{
			response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
			return;
		}
		
		long totalRam;
		long reservedRam;
		
		try
		{
			var option = Query.query(StartUpApplication.database, NodeTable.class)
					 .filter(NodeTable.NAME, foundGameServer.getColumnValue(GameServerTable.NODE_OWNER))
					 .first();
			
			if(option.isEmpty())
			{
				response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
				return;
			}
			
			var node = option.get();
	
			totalRam = node.getColumnValue(NodeTable.MAX_RAM_ALLOWED);
			reservedRam = StartUpApplication.getNodeReservedRam(node.getColumnValue(NodeTable.NAME));
		} catch (SQLException e1)
		{
			response.setStatus(500);
			return;
		}
		
		String nodeResponse;
		try
		{
			var url = nodeapi.ServerInteract.getEndpoint(serverID.get(), "properties");
			url.setHost(serverAddress);
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			var httpResponse = StartUpApplication.client.send(httpRequest, BodyHandlers.ofString());
			nodeResponse = httpResponse.body();
		}
		catch(InterruptedException e)
		{
			response.setStatus(500);
			return;
		}
		
		var defaultProperties = new TreeMap<String, Object>(MinecraftServer.MINECRAFT_PROPERTIES);
		
		defaultProperties.putAll(
			Stream.of(nodeResponse.split("\r\n"))
				  .map(line -> line.split("=", 2))
				  .filter(keyValue -> keyValue.length == 2)
				  .collect(Collectors.toMap(key -> key[0], value -> value[1]))
		);
		
		var properties = new LinkedList<PropertyInfo>();
		for(var entry : defaultProperties.entrySet())
		{
			properties.add(new PropertyInfo(entry.getKey(), Templates.minecraftPropertyToInputType(MinecraftServer.MINECRAFT_PROPERTIES.get(entry.getKey())), entry.getValue().toString()));
		}
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("serverID", serverID.get());
		context.put("serverName", foundGameServer.getColumnValue(GameServerTable.NAME));
		context.put("executableName", foundGameServer.getColumnValue(GameServerTable.EXECUTABLE_NAME));
		context.put("ramAmount", minecraftServerFound.getColumnValue(MinecraftServerTable.MAX_HEAP_SIZE));
		context.put("minimumRamAmount", MinecraftServer.MINIMUM_HEAP_SIZE);
		context.put("ramStep", MinecraftServer.HEAP_STEP);
		context.put("ramTotal", totalRam);
		context.put("ramAvailable", totalRam - reservedRam);
		context.put("arguments", minecraftServerFound.getColumnValue(MinecraftServerTable.ARGUMENTS));
		context.put("restarts", minecraftServerFound.getColumnValue(MinecraftServerTable.AUTO_RESTARTS));
		context.put("properties", properties);
		
		var template = Velocity.getTemplate("settings.vm");
		template.merge(context, response.getWriter());
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var execName = ApiSettings.EXECUTABLE_NAME.parse(request);
		if(!Utils.optionalsPresent(serverID))
		{
			response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
			return;
		}
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID.get());
		var serverType = StartUpApplication.serverTypes.get(serverID.get());
		if(serverAddress == null)
		{
			response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
			return;
		}
		
		var redirectURL = getEndpoint(serverID.get());
		
		if(!Utils.optionalsPresent(execName))
		{
			response.sendRedirect(redirectURL.getURL());
			return;
		}
		
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
							  .filter(GameServerTable.ID, serverID.get())
							  .first();
			
			if(option.isEmpty())
			{
				StartUpApplication.LOGGER.log(Level.SEVERE, "Server exists in cache, but not in the database!");
				response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
				return;
			}
			
			var gameServer = option.get();
			
			gameServer.setColumnValue(GameServerTable.EXECUTABLE_NAME, execName.get());
			
			if(serverType.equals(MinecraftServer.class))
			{
				var ramAmount = ApiSettings.RAM_AMOUNT.parse(request);
				var arguments = ApiSettings.ARGUMENTS.parse(request);
				var restarts = ApiSettings.RESTARTS_UNEXPECTED.parse(request);
				if(!Utils.optionalsPresent(ramAmount, arguments, restarts))
				{
					response.sendRedirect(redirectURL.getURL());
					return;
				}
				
				var option2 = Query.query(StartUpApplication.database, MinecraftServerTable.class)
								   .filter(MinecraftServerTable.SERVER_ID, gameServer.getColumnValue(GameServerTable.ID))
								   .first();
				
				if(option2.isEmpty())
				{
					StartUpApplication.LOGGER.log(Level.SEVERE, "Minecraft Server exists in cache, but not in the database!");
					response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
					return;
				}
				
				var minecraftServer = option2.get();
				
				minecraftServer.setColumnValue(MinecraftServerTable.MAX_HEAP_SIZE, ramAmount.get());
				minecraftServer.setColumnValue(MinecraftServerTable.ARGUMENTS, arguments.get());
				minecraftServer.setColumnValue(MinecraftServerTable.AUTO_RESTARTS, restarts.get());
				minecraftServer.commit(StartUpApplication.database);
			}
			
			gameServer.commit(StartUpApplication.database);
		}
		catch(SQLException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, "Failure to query/update database in [GameServerSettings]");
			response.sendRedirect(redirectURL.getURL());
			return;
		}
		
		var sendURL = nodeapi.ServerEdit.postEndpoint(serverID.get());
		sendURL.setHost(serverAddress);
		
		if(serverType.equals(MinecraftServer.class))
		{	
			// Server Properties
			var propertiesPost = MinecraftServer.MINECRAFT_PROPERTIES.keySet()
					.stream()
					.map(key -> {
						var property = Objects.requireNonNullElse(request.getParameter(key), "");
						return String.format("%s=%s", key, property);
					})
					.collect(Collectors.joining("&"));
			

			try
			{
				var url = nodeapi.ServerInteract.getEndpoint(serverID.get(), ApiSettings.PROPERTIES.getName());
				url.setHost(serverAddress);
				var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL()))
						.header("content-type", "application/x-www-form-urlencoded")
						.POST(BodyPublishers.ofString(propertiesPost))
						.build();
				StartUpApplication.client.send(httpRequest, BodyHandlers.discarding());
			}
			catch(InterruptedException e)
			{
				response.setStatus(500);
				return;
			}
		}
		
		try
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(sendURL.getURL()))
										 .POST(BodyPublishers.noBody())
										 .build();
			
			StartUpApplication.client.send(httpRequest, BodyHandlers.discarding());
		}
		catch(InterruptedException e)
		{
			response.setStatus(500);
			return;
		}
		
		doGet(request, response);
	}
}
