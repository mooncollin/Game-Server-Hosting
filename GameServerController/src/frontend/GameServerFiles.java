package frontend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.api.ServerInteract;
import backend.main.StartUpApplication;
import frontend.templates.GameServerFilesTemplate;
import model.Query;
import model.Table;
import models.GameServerTable;
import utils.MultipartInputStream;
import utils.Pair;

@WebServlet("/GameServerFiles")
@MultipartConfig
public class GameServerFiles extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerFiles";
	
	public static String getEndpoint(int serverID, String directory) 
	{
		return String.format("%s?id=%d&directory=%s", URL, serverID, directory);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverIDStr = request.getParameter("id");
		String directory = request.getParameter("directory");
		String redirectURL = request.getRequestURL() + "?" + request.getQueryString().replace("&folder=true", "");
		if(serverIDStr == null || directory == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		int serverID;
		try
		{
			serverID = Integer.parseInt(serverIDStr);
		}
		catch(NumberFormatException e)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var serverAddress = StartUpApplication.serverAddresses.get(serverID);
		var serverType = StartUpApplication.serverTypes.get(serverID);
		if(serverAddress == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		Table gameServer;
		
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
							  .filter(GameServerTable.ID, serverID)
							  .first();
			
			if(option.isEmpty())
			{
				response.sendRedirect(Index.URL);
				return;
			}
			
			gameServer = option.get();
		}
		catch(SQLException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var serverName = gameServer.getColumnValue(GameServerTable.NAME);
		
		if(directory.isEmpty())
		{
			directory = serverName;
		}
		
		var directories = directory.split(",");
		
		var files = new LinkedList<Pair<String, Boolean>>();
		
		try
		{
			var url = String.format("http://%s%s", serverAddress, api.ServerFiles.getEndpoint(directory));
			var httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
			var httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.ofString());
			if(httpResponse.statusCode() != 200)
			{
				StartUpApplication.LOGGER.log(Level.SEVERE, String.format("Got a non-200 status code from requesting the deployment folder of %s", serverName));
				response.setStatus(500);
				return;
			}
			
			if(!httpResponse.body().isBlank())
			{
				for(var file : httpResponse.body().split("\r\n|\n"))
				{
					var fileProperties = file.split(",");
					var fileName = fileProperties[0];
					var isDirectory = Boolean.parseBoolean(fileProperties[1]);
					
					files.add(Pair.of(fileName, isDirectory));
				}
			}
		}
		catch(InterruptedException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var template = new GameServerFilesTemplate
		(
				serverID, 
				serverType,
				serverName,
				directory,
				directories,
				redirectURL,
				files
		);
		
		response.setContentType("text/html");
		response.getWriter().print(template);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverIDStr = request.getParameter("id");
		var directory = request.getParameter("directory");
		var isFolder = request.getParameter("folder") != null;
		
		if(serverIDStr == null || directory == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		int serverID;
		
		try
		{
			serverID = Integer.parseInt(serverIDStr);
		}
		catch(NumberFormatException e)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var redirectURL = String.format("%s?%s", request.getRequestURL(), request.getQueryString().replace("&folder=true", ""));
		
		var serverAddress = StartUpApplication.serverAddresses.get(serverID);
		
		if(serverAddress == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var url = String.format("http://%s%s", serverAddress, api.FileUpload.getEndpoint(directory, isFolder).replace(' ', '+'));
		
		var fileParts = request.getParts()
							   .parallelStream()
							   .filter(p -> p.getSubmittedFileName() != null)
							   .collect(Collectors.toList());
		
		if(fileParts.size() > 0)
		{
			try(var multiInputStream = new MultipartInputStream(fileParts))
			{
				var httpRequest = HttpRequest.newBuilder(URI.create(url))
						.header("Content-type", "multipart/form-data; boundary=" + multiInputStream.getBoundary())
						.POST(BodyPublishers.ofInputStream(() -> multiInputStream))
						.build();
				try
				{
					ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
				} catch (InterruptedException e)
				{
				}
			}
		}
		
		response.sendRedirect(redirectURL);
	}
}
