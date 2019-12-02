package frontend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import api.minecraft.MinecraftServer;
import api.minecraft.MinecraftServerCommandHandler;
import backend.api.ServerInteract;
import backend.main.StartUpApplication;
import frontend.templates.GameServerSettingsTemplate;
import model.Query;
import model.Table;
import model.Filter.FilterType;
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
		url.addQuery(ApiSettings.SERVER_ID_PARAMETER, id);
		return url;
	}
	
	public static ParameterURL postEndpoint(int id)
	{
		return getEndpoint(id);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		if(serverID == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID);
		var serverType = StartUpApplication.serverTypes.get(serverID);
		if(serverAddress == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		Table foundGameServer;
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
					   			   .filter(GameServerTable.ID.cloneWithValue(serverID))
					   			   .first();
			if(option.isEmpty())
			{
				response.sendRedirect(Index.URL);
				return;
			}
			else
			{
				foundGameServer = option.get();
			}
		} catch (SQLException e)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		Table minecraftServerFound;
		try
		{
			var option = Query.query(StartUpApplication.database, MinecraftServerTable.class)
										.join(foundGameServer, GameServerTable.ID, FilterType.EQUAL, new MinecraftServerTable(), MinecraftServerTable.ID)
										.join(foundGameServer, foundGameServer.getColumn(GameServerTable.ID), FilterType.EQUAL)
										.first();
			if(option.isEmpty())
			{
				response.sendRedirect(Index.URL);
				return;
			}
			else
			{
				minecraftServerFound = option.get();
			}
			
		} catch (SQLException e2)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		long totalRam;
		long reservedRam;
		
		try
		{
			var option = Query.query(StartUpApplication.database, NodeTable.class)
					 .filter(NodeTable.NAME.cloneWithValue(foundGameServer.getColumnValue(GameServerTable.NODE_OWNER)))
					 .first();
			
			Table node;
			
			if(option.isEmpty())
			{
				response.sendRedirect(Index.URL);
				return;
			}
			else
			{
				node = option.get();
			}
	
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
			var url = nodeapi.ServerInteract.getEndpoint(serverID, "properties");
			url.setHost(serverAddress);
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			var httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.ofString());
			nodeResponse = httpResponse.body();
		}
		catch(InterruptedException e)
		{
			response.setStatus(500);
			return;
		}
		
		var defaultProperties = new HashMap<String, Object>(MinecraftServer.MINECRAFT_PROPERTIES);
		
		defaultProperties.putAll(
			Stream.of(nodeResponse.split("\r\n"))
				  .filter(line -> !line.isBlank())
				  .map(line -> line.split("=", 2))
				  .filter(keyValue -> keyValue.length == 2)
				  .collect(Collectors.toMap(key -> key[0], value -> value[1]))
		);
		
		var template = new GameServerSettingsTemplate(serverType, foundGameServer, 
				minecraftServerFound, totalRam, reservedRam, defaultProperties);
		
		response.getWriter().print(template);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		if(serverID == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID);
		var serverType = StartUpApplication.serverTypes.get(serverID);
		if(serverAddress == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var redirectURL = getEndpoint(serverID);
		
		var execName = request.getParameter(ApiSettings.EXECUTABLE_NAME_PARAMETER);
		if(execName == null || execName.isBlank())
		{
			response.sendRedirect(redirectURL.getURL());
			return;
		}
		
		
		var sendURL = nodeapi.ServerEdit.postEndpoint(serverID, execName);
		sendURL.setHost(serverAddress);
		
		if(serverType.equals(MinecraftServer.class))
		{
			
			var ramAmount = Utils.fromString(Long.class, request.getParameter(MinecraftServer.RAM_AMOUNT_PARAMETER));
			if(ramAmount == null)
			{
				response.sendRedirect(redirectURL.getURL());
				return;
			}
			
			if(ramAmount < MinecraftServer.MINIMUM_HEAP_SIZE || ramAmount % 1024 != 0)
			{
				response.sendRedirect(redirectURL.getURL());
				return;
			}
			
			sendURL.addQuery(MinecraftServer.RAM_AMOUNT_PARAMETER, ramAmount);
			
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
				var url = nodeapi.ServerInteract.getEndpoint(serverID, MinecraftServerCommandHandler.PROPERTIES_COMMAND);
				url.setHost(serverAddress);
				var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL()))
						.header("content-type", "application/x-www-form-urlencoded")
						.POST(BodyPublishers.ofString(propertiesPost))
						.build();
				ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
			}
			catch(InterruptedException e)
			{
				response.setStatus(500);
				return;
			}
			
			try
			{
				var url = nodeapi.ServerInteract.getEndpoint(serverID, MinecraftServerCommandHandler.RESTARTS_COMMAND);
				url.setHost(serverAddress);
				if(request.getParameter(MinecraftServer.RESTART_PARAMETER) != null)
				{
					url.addQuery(MinecraftServer.RESTART_PARAMETER, "on");
				}
				
				var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL()))
						.header("content-type", "application/x-www-form-urlencoded")
						.POST(BodyPublishers.noBody())
						.build();
				ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
			}
			catch(InterruptedException e)
			{
				response.setStatus(500);
			}
			
			try
			{
				var url = nodeapi.ServerInteract.getEndpoint(serverID, MinecraftServer.ARGUMENTS_PARAMETER);
				url.setHost(serverAddress);
				url.addQuery(MinecraftServer.ARGUMENTS_PARAMETER, Objects.requireNonNullElse(request.getParameter(MinecraftServer.ARGUMENTS_PARAMETER), ""));
				var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL()))
						.header("content-type", "application/x-www-form-urlencoded")
						.POST(BodyPublishers.noBody())
						.build();
				ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
			}
			catch(InterruptedException e)
			{
				response.setStatus(500);
			}
		}
		
		try
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(sendURL.getURL()))
										 .POST(BodyPublishers.noBody())
										 .build();
			
			ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
		}
		catch(InterruptedException e)
		{
			response.setStatus(500);
			return;
		}
		
		doGet(request, response);
	}
}
