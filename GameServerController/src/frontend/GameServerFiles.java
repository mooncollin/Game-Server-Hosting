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
import nodeapi.ApiSettings;
import utils.MultipartInputStream;
import utils.Pair;
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/GameServerFiles")
@MultipartConfig
public class GameServerFiles extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameServerFiles";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL getEndpoint(int serverID, String directory) 
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID_PARAMETER, serverID);
		url.addQuery(ApiSettings.DIRECTORY_PARAMETER, directory);
		return url;
	}
	
	public static ParameterURL postEndpoint(int serverID, String directory, String folder)
	{
		var url = getEndpoint(serverID, directory);
		if(folder != null)
		{
			url.addQuery(ApiSettings.FOLDER_PARAMETER, folder);
		}
		return url;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		var directory = request.getParameter(ApiSettings.DIRECTORY_PARAMETER);
		
		var redirectURL = new ParameterURL(request.getScheme(), request.getServerName(), request.getServerPort(), StartUpApplication.SERVLET_PATH + request.getServletPath());
		redirectURL.addQuery(ApiSettings.SERVER_ID_PARAMETER, serverID);
		redirectURL.addQuery(ApiSettings.DIRECTORY_PARAMETER, directory);
		
		if(serverID == null || directory == null)
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
			var url = nodeapi.ServerFiles.getEndpoint(directory);
			url.setHost(serverAddress);
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
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
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		var directory = request.getParameter(ApiSettings.DIRECTORY_PARAMETER);
		var isFolder = request.getParameter(ApiSettings.FOLDER_PARAMETER) != null;
		
		if(serverID == null || directory == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var redirectURL = new ParameterURL(request.getScheme(), request.getServerName(), request.getServerPort(), StartUpApplication.SERVLET_PATH + request.getServletPath());
		redirectURL.addQuery(ApiSettings.SERVER_ID_PARAMETER, serverID);
		redirectURL.addQuery(ApiSettings.DIRECTORY_PARAMETER, directory);
		
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID);
		
		if(serverAddress == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var url = nodeapi.FileUpload.postEndpoint(directory, isFolder);
		url.setHost(serverAddress);
		
		var fileParts = request.getParts()
							   .parallelStream()
							   .filter(p -> p.getSubmittedFileName() != null)
							   .collect(Collectors.toList());
		
		if(fileParts.size() > 0)
		{
			try(var multiInputStream = new MultipartInputStream(fileParts))
			{
				var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL()))
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
		
		response.sendRedirect(redirectURL.getURL());
	}
}
