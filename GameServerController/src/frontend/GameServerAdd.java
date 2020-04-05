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

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import api.minecraft.MinecraftServer;
import backend.main.StartUpApplication;
import frontend.templates.Templates.NodeInfo;
import model.Query;
import models.GameServerTable;
import models.NodeTable;
import nodeapi.ApiSettings;
import server.GameServer;
import utils.MultipartInputStream;
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/GameServerAdd")
@MultipartConfig
public class GameServerAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameServerAdd";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL getEndpoint()
	{
		var url = new ParameterURL(PARAMETER_URL);
		return url;
	}
	
	public static ParameterURL postEndpoint(String serverName, String execName, String nodeName, String type)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_NAME.getName(), serverName);
		url.addQuery(ApiSettings.EXECUTABLE_NAME.getName(), execName);
		url.addQuery(ApiSettings.NODE_NAME.getName(), nodeName);
		url.addQuery(ApiSettings.SERVER_TYPE.getName(), type);
		
		return url;
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var nodes = new LinkedList<NodeInfo>();
		try
		{
			var nodeRows = Query.query(StartUpApplication.database, NodeTable.class)
							 .all();
			
			for(var node : nodeRows)
			{
				var name = node.getColumnValue(NodeTable.NAME);
				nodes.add(new NodeInfo(name,
									   node.getColumnValue(NodeTable.MAX_RAM_ALLOWED),
									   StartUpApplication.getNodeReservedRam(name)));
			}
			
		} catch (SQLException | RuntimeException e1)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e1.getMessage());
			response.setStatus(500);
			return;
		}
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("nodes", nodes);
		context.put("serverTypes", GameServer.PROPERTY_NAMES_TO_TYPE.keySet());
		context.put("minRamAmount", MinecraftServer.MINIMUM_HEAP_SIZE);
		context.put("defaultRamAmount", MinecraftServer.MINIMUM_HEAP_SIZE);
		context.put("ramStep", MinecraftServer.HEAP_STEP);
		
		var template = Velocity.getTemplate("addServer.vm");
		template.merge(context, response.getWriter());
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverName = ApiSettings.SERVER_NAME.parse(request);
		var executableName = ApiSettings.EXECUTABLE_NAME.parse(request);
		var nodeName = ApiSettings.NODE_NAME.parse(request);
		var type = ApiSettings.SERVER_TYPE.parse(request);
		
		if(!Utils.optionalsPresent(serverName, executableName, nodeName, type))
		{
			doGet(request, response);
			return;
		}
		
		var serverAddress = StartUpApplication.nodeIPAddresses.get(nodeName.get());
		
		if(serverAddress == null)
		{
			doGet(request, response);
			return;
		}
		
		try
		{
			var gameServer = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.NAME.cloneWithValue(serverName.get()))
								  .first();
			
			if(gameServer.isPresent())
			{
				doGet(request, response);
				return;
			}
		}
		catch(SQLException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var url = nodeapi.ServerAdd.postEndpoint(serverName.get(), executableName.get(), type.get());
		url.setHost(serverAddress);
		
		if(type.get().equals("minecraft"))
		{
			var ram = ApiSettings.RAM_AMOUNT.parse(request);
			if(ram.isEmpty())
			{
				doGet(request, response);
				return;
			}
			
			var restart = ApiSettings.RESTARTS_UNEXPECTED.parse(request);
			
			url.addQuery(ApiSettings.RAM_AMOUNT.getName(), ram.get());
			url.addQuery(ApiSettings.RESTARTS_UNEXPECTED.getName(), restart.get());
		}
		
		var fileParts = request.getParts()
							   .parallelStream()
							   .filter(p -> p.getSubmittedFileName() != null)
							   .collect(Collectors.toList());
		
		try(var multiInputStream = new MultipartInputStream(fileParts))
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL()))
					.header("Content-type", "multipart/form-data; boundary=" + multiInputStream.getBoundary())
					.POST(BodyPublishers.ofInputStream(() -> multiInputStream))
					.build();
			
			try
			{
				var httpResponse = StartUpApplication.client.send(httpRequest, BodyHandlers.ofString());
				if(httpResponse.statusCode() == 200)
				{
					var id = Integer.parseInt(httpResponse.body());
					StartUpApplication.serverTypes.put(id, GameServer.PROPERTY_NAMES_TO_TYPE.get(type.get()));
					StartUpApplication.serverIPAddresses.put(id, serverAddress);
					response.sendRedirect(StartUpApplication.getUrlMapping(Index.class));
					return;
				}
			}
			catch(InterruptedException e)
			{
			}
		}
		
		doGet(request, response);
	}
}
