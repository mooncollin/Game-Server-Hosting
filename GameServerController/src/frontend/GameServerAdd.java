package frontend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.util.List;
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
import frontend.templates.Templates.NodeInfo;
import model.Query;
import models.GameServerTable;
import models.NodeTable;
import nodeapi.ApiSettings;
import utils.MultipartInputStream;
import utils.Utils;

/**
 * The frontend for adding a game server. Responsible for adding the game server to the database
 * and relaying the newly added server to the corresponding node.
 * @author Collin
 *
 */
@WebServlet(
		name = "GameServerAdd",
		urlPatterns = "/GameServerAdd",
		asyncSupported = true
)
@MultipartConfig
public class GameServerAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		List<NodeInfo> nodes;
		try
		{
			var nodeRows = Query.query(StartUpApplication.database, NodeTable.class)
							 .all();
			
			nodes = nodeRows.parallelStream()
							.map(row -> new NodeInfo(row.getColumnValue(NodeTable.NAME)))
							.collect(Collectors.toList());
			
		} catch (SQLException | RuntimeException e1)
		{
			StartUpApplication.LOGGER.error(e1.getMessage());
			response.setStatus(500);
			return;
		}
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("nodes", nodes);
		context.put("serverTypes", StartUpApplication.getServerTypes());
		context.put("namePattern", ApiSettings.SERVER_NAME_PATTERN.pattern());
		
		var template = Velocity.getTemplate("addServer.vm");
		template.merge(context, response.getWriter());
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverName = ApiSettings.SERVER_NAME.parse(request);
		var executableName = ApiSettings.EXECUTABLE_NAME.parse(request);
		var nodeName = ApiSettings.NODE_NAME.parse(request);
		var type = ApiSettings.SERVER_TYPE.parse(request);
		var restart = ApiSettings.RESTARTS_UNEXPECTED.parse(request);
		
		if(!Utils.optionalsPresent(serverName, executableName, nodeName, type) ||
			StartUpApplication.getModule(type.get()).gameServerOptions().supportsAutoRestart() && restart.isEmpty())
		{
			response.sendRedirect(Endpoints.GAME_SERVER_ADD.get().getURL());
			return;
		}
		
		var serverAddress = StartUpApplication.getNodeIPAddress(nodeName.get());
		if(serverAddress == null)
		{
			response.sendRedirect(Endpoints.GAME_SERVER_ADD.get().getURL());
			return;
		}
		
		GameServerTable gameServer;
		
		try
		{
			var gameServerOptional = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.NAME.cloneWithValue(serverName.get()))
								  .first();
			
			if(gameServerOptional.isPresent())
			{
				response.sendRedirect(Endpoints.GAME_SERVER_ADD.get().getURL());
				return;
			}
			
			gameServer = new GameServerTable();
			gameServer.setColumnValue(GameServerTable.NAME, serverName.get());
			gameServer.setColumnValue(GameServerTable.NODE_OWNER, nodeName.get());
			gameServer.setColumnValue(GameServerTable.SERVER_TYPE, type.get());
			gameServer.setColumnValue(GameServerTable.EXECUTABLE_NAME, executableName.get());
			if(StartUpApplication.getModule(type.get()).gameServerOptions().supportsAutoRestart())
			{
				gameServer.setColumnValue(GameServerTable.AUTO_RESTARTS, restart.get());
			}
			
			gameServer.commit(StartUpApplication.database);
			StartUpApplication.getModule(type.get()).gameServerOptions().addGameServerTable(gameServer, StartUpApplication.database);
		}
		catch(SQLException e)
		{
			StartUpApplication.LOGGER.error(e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var url = nodeapi.ServerAdd.postEndpoint(gameServer.getColumnValue(GameServerTable.ID));
		url.setHost(serverAddress);
		
		var fileParts = request.getParts()
							   .parallelStream()
							   .collect(Collectors.toList());
		
		try(var multiInputStream = new MultipartInputStream(fileParts))
		{
			var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL()))
					.header("Content-type", "multipart/form-data; boundary=" + multiInputStream.getBoundary())
					.POST(BodyPublishers.ofInputStream(() -> multiInputStream))
					.build();
		
			try
			{
				StartUpApplication.client.send(httpRequest, BodyHandlers.discarding());
				StartUpApplication.addServerIPAddress(gameServer.getColumnValue(GameServerTable.ID), nodeName.get());
			}
			catch(InterruptedException e)
			{
			}
		}
		
		response.sendRedirect(Endpoints.INDEX.get().getURL());
	}
}
