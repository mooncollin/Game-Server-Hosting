package frontend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import api.minecraft.MinecraftServer;
import backend.api.ServerInteract;
import backend.main.StartUpApplication;
import forms.Form;
import forms.Input;
import forms.TextField;
import html.CompoundElement;
import html.Element;
import model.Query;
import model.Table;
import models.GameServerTable;
import models.MinecraftServerTable;
import models.NodeTable;
import tags.Anchor;
import tags.Button;
import util.Template;
import utils.Utils;

@WebServlet("/GameServerSettings")
public class GameServerSettings extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerSettings";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
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
			foundGameServer = Query.query(StartUpApplication.database, GameServerTable.class)
					   			   .filter(GameServerTable.NAME.cloneWithValue(serverName))
					   			   .first();
			if(foundGameServer == null)
			{
				response.sendRedirect(Index.URL);
				return;
			}
		} catch (SQLException e)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		Template template = Templates.getMainTemplate();
		CompoundElement content = (CompoundElement) template.getBody().getElementById("content");
		if(serverFound.getFirst().equals(MinecraftServer.class))
		{
			content.setStyle("background-image: url('images/minecraft/settings_background.jpg')");
		}
		
		CompoundElement header = new CompoundElement("h1", serverName);
		header.addClasses("text-light", "ml-5", "mt-4", "d-inline-block");
		content.addElement(header);
		
		CompoundElement linksContainer = new CompoundElement("div");
		linksContainer.addClasses("float-right", "mt-5", "mr-5");
		
		Anchor consoleLink = Templates.generateConsoleLink(serverName);
		consoleLink.addClasses("mr-4");
		Anchor filesLink = Templates.generateFilesLink(serverName);
		filesLink.addClasses("mr-4");
		Anchor settingsLink = Templates.generateSettingsLink(serverName);
		
		linksContainer.addElement(consoleLink);
		linksContainer.addElement(filesLink);
		linksContainer.addElement(settingsLink);
	
		content.addElement(linksContainer);
		content.addElement(new Element("hr"));
		
		CompoundElement container = new CompoundElement("div");
		container.addClasses("container", "bg-dark");
		
		Form settingsForm = new Form();
		settingsForm.setMethod("POST");
		settingsForm.setID("settingsForm");
		
		CompoundElement settingsList = new CompoundElement("ul");
		settingsList.addClasses("list-group", "list-group-flush");
		
		CompoundElement optionsItem = new CompoundElement("li");
		optionsItem.addClasses("list-group-item", "form-group", "rounded", "mr-5");
		Button editButton = new Button();
		editButton.addClasses("btn", "btn-warning");
		editButton.setOnClick("edit()");
		editButton.setID("editButton");
		editButton.setType("button");
		CompoundElement editIcon = new CompoundElement("i");
		editIcon.addClasses("fas", "fa-pencil-alt");
		
		editButton.addElement(editIcon);
		optionsItem.addElement(editButton);
		
		CompoundElement submitButton = new Button();
		submitButton.addClasses("btn", "btn-success", "ml-3");
		submitButton.setID("submitButton");
		submitButton.setHidden(true);
		submitButton.setAttribute("form", "settingsForm");
		CompoundElement submitIcon = new CompoundElement("i");
		submitIcon.addClasses("fas", "fa-check");
		
		submitButton.addElement(submitIcon);
		optionsItem.addElement(submitButton);
		
		CompoundElement optionsList = new CompoundElement("ul");
		optionsList.addClasses("list-group", "list-group-flush", "float-right", "sticky-top");
		optionsList.addElement(optionsItem);
		
		CompoundElement settingsListItem = new CompoundElement("li");
		settingsListItem.addClass("list-group-item");
		settingsListItem.addElement(new CompoundElement("h1", "Settings"));
		settingsList.addElement(settingsListItem);
		
		settingsList.addElement(BootstrapTemplates.settingsInput(TextField.class, "Executable Name", "execName", "Enter name of server executable", foundGameServer.getColumn(GameServerTable.EXECUTABLE_NAME).getValue(), null, true, true));
		
		if(serverFound.getFirst().equals(MinecraftServer.class))
		{
			Table minecraftServerFound;
			try
			{
				minecraftServerFound = Query.query(StartUpApplication.database, MinecraftServerTable.class)
											.filter(MinecraftServerTable.ID.cloneWithValue(foundGameServer.getColumnValue(GameServerTable.SPECIFIC_ID)))
											.first();
			} catch (SQLException e2)
			{
				response.sendRedirect(Index.URL);
				return;
			}
			
			CompoundElement heapGroup = BootstrapTemplates.settingsInput(forms.Number.class, "Maximum RAM (MB)", "ramAmount", "Enter maximum amount of ram to use in MB", String.valueOf(minecraftServerFound.getColumn(MinecraftServerTable.MAX_HEAP_SIZE).getValue()), "RAM must be in 1024 increments", true, true);
			forms.Number heapInput = (forms.Number) heapGroup.getElementById("ramAmount");
			heapInput.setMin(MinecraftServer.MINIMUM_HEAP_SIZE);
			heapInput.setStep(1024);
			
			long totalRam;
			long reservedRam;
			
			try
			{
				var node = Query.query(StartUpApplication.database, NodeTable.class)
						 .filter(NodeTable.NAME.cloneWithValue(foundGameServer.getColumnValue(GameServerTable.NODE_OWNER)))
						 .first();
		
				totalRam = node.getColumnValue(NodeTable.MAX_RAM_ALLOWED);
				reservedRam = StartUpApplication.getNodeReservedRam(node.getColumnValue(NodeTable.NAME));
			} catch (SQLException e1)
			{
				response.setStatus(500);
				return;
			}
			
			CompoundElement nodeUsageSmall = new CompoundElement("small", String.format("This node has a total of %d MB of RAM. Memory Available: %d MB", totalRam, totalRam - reservedRam));
			nodeUsageSmall.addClasses("form-text", "text-muted");
			heapGroup.addElement(nodeUsageSmall);
			settingsList.addElement(heapGroup);
			
			CompoundElement argumentsGroup = BootstrapTemplates.settingsInput(TextField.class, "Extra JVM Arguments", "arguments", "Enter optional arguments", minecraftServerFound.getColumn(MinecraftServerTable.ARGUMENTS).getValue(), "These will be used when executing server start. Options -Xmx and -Xms are already set using the RAM given.", false, true);
			TextField argumentInput = (TextField) argumentsGroup.getElementById("arguments");
			argumentInput.removeClass("w-25");
			argumentInput.addClass("w-100");
			settingsList.addElement(argumentsGroup);
			
			boolean restarts = minecraftServerFound.getColumn(MinecraftServerTable.AUTO_RESTARTS).getValue();
			
			CompoundElement restartGroup = BootstrapTemplates.settingsInput(forms.Checkbox.class, "Automatic Restart", "restartsUnexpected", null, String.valueOf(restarts), null, false, true);
			settingsList.addElement(restartGroup);
			
			CompoundElement serverPropertiesListItem = new CompoundElement("li");
			serverPropertiesListItem.addClass("list-group-item");
			serverPropertiesListItem.addElement(new CompoundElement("h1", "Server Properties"));
			
			settingsList.addElement(serverPropertiesListItem);
			
			// Get values from node
			String nodeResponse;
			try
			{
				String url = String.format("http://%s/ServerInteract?name=%s&command=properties", serverFound.getSecond(), serverName.replace(' ', '+'));
				HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
				HttpResponse<String> httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.ofString());
				nodeResponse = httpResponse.body();
			}
			catch(InterruptedException e)
			{
				response.setStatus(500);
				return;
			}
			
			Map<String, Object> defaultProperties = new HashMap<String, Object>(MinecraftServer.MINECRAFT_PROPERTIES);
			
			for(String line : nodeResponse.split("\r\n"))
			{
				if(!line.isBlank())
				{
					String[] keyValue = line.split("=", 2);
					if(keyValue.length == 2)
					{
						defaultProperties.replace(keyValue[0], keyValue[1]);
					}
				}
			}
			
			var properties = new LinkedList<String>(defaultProperties.keySet());
			Collections.sort(properties);
			
			for(String key : properties)
			{
				CompoundElement propGroup = BootstrapTemplates.settingsInput(minecraftPropertyToInput(MinecraftServer.MINECRAFT_PROPERTIES.get(key)),
						key, key, "Enter " + key, String.valueOf(defaultProperties.get(key)), null, false, true);
				settingsList.addElement(propGroup);
			}
		}
		
		settingsForm.addElement(settingsList);
		container.addElement(settingsForm);
		content.addElement(optionsList);
		content.addElement(container);
		template.getBody().addScript("js/settings.js");
		
		response.setContentType("text/html");
		response.getWriter().print(template);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
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
		
		String redirectURL = String.format("%s?name=%s", GameServerSettings.URL, serverName.replace(' ', '+'));
		String execName = request.getParameter("execName");
		if(execName == null || execName.isBlank())
		{
			response.sendRedirect(redirectURL);
			return;
		}
		
		
		String sendURL = String.format("http://%s/ServerEdit?name=%s&execName=%s", serverFound.getSecond(), serverName.replace(' ', '+'), execName.replace(' ', '+'));
		
		if(serverFound.getFirst().equals(MinecraftServer.class))
		{
			
			String ramAmountStr = request.getParameter("ramAmount");
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
			String propertiesPost = "";
			for(String key : MinecraftServer.MINECRAFT_PROPERTIES.keySet())
			{
				String property = request.getParameter(key);
				if(property == null)
				{
					property = "";
				}
				propertiesPost += String.format("%s=%s&", key, property);
			}
			
			propertiesPost = propertiesPost.substring(0, propertiesPost.length() - 1);
			try
			{
				final String url = Utils.encodeURL(String.format("http://%s/ServerInteract?name=%s&command=properties", serverFound.getSecond(), serverName));
				HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
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
			
			String restartPost = request.getParameter("restartsUnexpected") == null ? "" : "restartsUnexpected=on";
			try
			{
				final String url = Utils.encodeURL(String.format("http://%s/ServerInteract?name=%s&command=restarts", serverFound.getSecond(), serverName));
				HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
						.header("content-type", "application/x-www-form-urlencoded")
						.POST(BodyPublishers.ofString(restartPost))
						.build();
				ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
			}
			catch(InterruptedException e)
			{
				response.setStatus(500);
			}
			
			String argumentsPost = ("arguments=" + Objects.requireNonNullElse(request.getParameter("arguments"), "").replace("+", "%2B")).strip();
			try
			{
				final String url = Utils.encodeURL(String.format("http://%s/ServerInteract?name=%s&command=arguments", serverFound.getSecond(), serverName));
				HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
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
			HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(sendURL)).build();
			ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
		}
		catch(InterruptedException e)
		{
			response.setStatus(500);
			return;
		}
		
		doGet(request, response);
	}
	
	private static Class<? extends Input> minecraftPropertyToInput(Object prop)
	{
		Class<? extends Input> clazz = forms.TextField.class;
		if(prop instanceof Integer)
		{
			clazz = forms.Number.class;
		}
		else if(prop instanceof Boolean)
		{
			clazz = forms.Checkbox.class;
		}
		
		return clazz;
	}
}
