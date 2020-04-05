package frontend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import backend.main.StartUpApplication;
import frontend.templates.Templates.DirectoryEntry;
import frontend.templates.Templates.FileInfo;
import model.Query;
import model.Table;
import models.GameServerTable;
import nodeapi.ApiSettings;
import utils.MultipartInputStream;
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
	
	public static ParameterURL getEndpoint(int serverID, String[] directories) 
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
		url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", Arrays.asList(directories)));
		return url;
	}
	
	public static ParameterURL postEndpoint(int serverID, String[] directories, String folder)
	{
		var url = getEndpoint(serverID, directories);
		if(folder != null)
		{
			url.addQuery(ApiSettings.FOLDER.getName(), folder);
		}
		return url;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var directory = ApiSettings.DIRECTORY.parse(request);
		
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
		
		Table gameServer;
		
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
							  .filter(GameServerTable.ID, serverID.get())
							  .first();
			
			if(option.isEmpty())
			{
				response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
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
		var directories = directory.isPresent() ? directory.get() : new String[] {serverName};
		
		var redirectURL = new ParameterURL(request.getScheme(), request.getServerName(), request.getServerPort(), StartUpApplication.SERVLET_PATH + request.getServletPath());
		redirectURL.addQuery(ApiSettings.SERVER_ID.getName(), serverID.get());
		redirectURL.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
		
		var files = new LinkedList<FileInfo>();
		
		try
		{
			var url = nodeapi.ServerFiles.getEndpoint(directories);
			url.setHost(serverAddress);
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL())).build();
			var httpResponse = StartUpApplication.client.send(httpRequest, BodyHandlers.ofString());
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
					
					files.add(new FileInfo(fileName, isDirectory));
				}
			}
		}
		catch(InterruptedException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var directoryList = new LinkedList<DirectoryEntry>();
		var currentPath = new String[] {};
		for(var dir : directory.get())
		{
			directoryList.add(new DirectoryEntry(dir, currentPath));
			currentPath = Utils.concatenate(currentPath, dir);
		}
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("serverName", serverName);
		context.put("serverID", serverID.get());
		context.put("directoryList", directoryList);
		context.put("directories", String.join(",", directory.get()));
		context.put("redirectURL", redirectURL);
		context.put("files", files);
		
		var template = Velocity.getTemplate("files.vm");
		template.merge(context, response.getWriter());
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var isFolder = ApiSettings.FOLDER.parse(request);
		var directory = ApiSettings.DIRECTORY.parse(request);
		
		if(!Utils.optionalsPresent(serverID, isFolder, directory))
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
		
		var url = nodeapi.FileUpload.postEndpoint(directory.get(), isFolder.get());
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
					StartUpApplication.client.send(httpRequest, BodyHandlers.discarding());
				} catch (InterruptedException e)
				{
				}
			}
		}
		
		response.sendRedirect(getEndpoint(serverID.get(), directory.get()).getURL());
	}
}
