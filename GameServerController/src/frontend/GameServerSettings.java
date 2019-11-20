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
import backend.api.ServerInteract;
import backend.main.StartUpApplication;
import frontend.templates.GameServerSettingsTemplate;
import model.Query;
import model.Table;
import model.Filter.FilterType;
import models.GameServerTable;
import models.MinecraftServerTable;
import models.NodeTable;
import utils.Utils;

@WebServlet("/GameServerSettings")
public class GameServerSettings extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerSettings";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverName = request.getParameter("name");
		if(serverName == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var serverFound = StartUpApplication.getServerInfo().get(serverName);
		if(serverFound == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		Table foundGameServer;
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
					   			   .filter(GameServerTable.NAME.cloneWithValue(serverName))
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
			var url = String.format("http://%s/ServerInteract?name=%s&command=properties", serverFound.getSecond(), serverName.replace(' ', '+'));
			var httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
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
		
		var template = new GameServerSettingsTemplate(serverName, 
				serverFound.getFirst(), foundGameServer, 
				minecraftServerFound, totalRam, reservedRam, defaultProperties);
		
		response.getWriter().print(template);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		final var serverName = request.getParameter("name");
		if(serverName == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		final var serverFound = StartUpApplication.getServerInfo().get(serverName);
		if(serverFound == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		final var redirectURL = String.format("%s?name=%s", GameServerSettings.URL, serverName.replace(' ', '+'));
		final var execName = request.getParameter("execName");
		if(execName == null || execName.isBlank())
		{
			response.sendRedirect(redirectURL);
			return;
		}
		
		
		var sendURL = String.format("http://%s/ServerEdit?name=%s&execName=%s", serverFound.getSecond(), serverName.replace(' ', '+'), execName.replace(' ', '+'));
		
		if(serverFound.getFirst().equals(MinecraftServer.class))
		{
			
			final var ramAmountStr = request.getParameter("ramAmount");
			int ramAmount;
			if(ramAmountStr == null || ramAmountStr.isBlank())
			{
				response.sendRedirect(redirectURL);
				return;
			}
			
			try
			{
				ramAmount = Integer.valueOf(ramAmountStr);
			}
			catch(NumberFormatException e)
			{
				response.sendRedirect(redirectURL);
				return;
			}
			
			if(ramAmount < MinecraftServer.MINIMUM_HEAP_SIZE || ramAmount % 1024 != 0)
			{
				response.sendRedirect(redirectURL);
				return;
			}
			
			sendURL += String.format("&ramAmount=%s", ramAmount);
			
			// Server Properties
			final var propertiesPost = MinecraftServer.MINECRAFT_PROPERTIES.keySet()
					.stream()
					.map(key -> {
						var property = Objects.requireNonNullElse(request.getParameter(key), "");
						return String.format("%s=%s", key, property);
					})
					.collect(Collectors.joining("&"));
			

			try
			{
				final var url = Utils.encodeURL(String.format("http://%s/ServerInteract?name=%s&command=properties", serverFound.getSecond(), serverName));
				var httpRequest = HttpRequest.newBuilder(URI.create(url))
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
				final var restartPost = request.getParameter("restartsUnexpected") == null ? "" : "restartsUnexpected=on";
				final var url = Utils.encodeURL(String.format("http://%s/ServerInteract?name=%s&command=restarts", serverFound.getSecond(), serverName));
				var httpRequest = HttpRequest.newBuilder(URI.create(url))
						.header("content-type", "application/x-www-form-urlencoded")
						.POST(BodyPublishers.ofString(restartPost))
						.build();
				ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
			}
			catch(InterruptedException e)
			{
				response.setStatus(500);
			}
			
			try
			{
				final var argumentsPost = ("arguments=" + Objects.requireNonNullElse(request.getParameter("arguments"), "").replace("+", "%2B")).strip();
				final var url = Utils.encodeURL(String.format("http://%s/ServerInteract?name=%s&command=arguments", serverFound.getSecond(), serverName));
				var httpRequest = HttpRequest.newBuilder(URI.create(url))
						.header("content-type", "application/x-www-form-urlencoded")
						.POST(BodyPublishers.ofString(argumentsPost))
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
			var httpRequest = HttpRequest.newBuilder(URI.create(sendURL)).build();
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
