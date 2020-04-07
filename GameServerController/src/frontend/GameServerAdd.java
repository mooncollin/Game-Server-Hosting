package frontend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
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
import utils.MultipartInputStream;
import utils.Utils;

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
		Set<String> serverTypes;
		try
		{
			var nodeRows = Query.query(StartUpApplication.database, NodeTable.class)
							 .all();
			
			nodes = nodeRows.parallelStream()
							.map(row -> {
								var name = row.getColumnValue(NodeTable.NAME);
									try
									{
										return new NodeInfo(name,
															row.getColumnValue(NodeTable.MAX_RAM_ALLOWED),
															StartUpApplication.getNodeReservedRam(name));
									} catch (SQLException e)
									{
										throw new RuntimeException(e.getMessage());
									}
							})
							.collect(Collectors.toList());
			
			serverTypes = models.Utils.getServerTypes();
			
		} catch (SQLException | RuntimeException e1)
		{
			StartUpApplication.LOGGER.error(e1.getMessage());
			response.setStatus(500);
			return;
		}
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("nodes", nodes);
		context.put("serverTypes", serverTypes);
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
			response.sendRedirect(Endpoints.GAME_SERVER_ADD.get().getURL());
			return;
		}
		
		var serverAddress = StartUpApplication.getNodeIPAddress(nodeName.get());
		if(serverAddress == null)
		{
			response.sendRedirect(Endpoints.GAME_SERVER_ADD.get().getURL());
			return;
		}
		
		try
		{
			var gameServer = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.NAME.cloneWithValue(serverName.get()))
								  .first();
			
			if(gameServer.isPresent())
			{
				response.sendRedirect(Endpoints.GAME_SERVER_ADD.get().getURL());
				return;
			}
		}
		catch(SQLException e)
		{
			StartUpApplication.LOGGER.error(e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var url = nodeapi.ServerAdd.postEndpoint(serverName.get(), executableName.get(), type.get());
		url.setHost(serverAddress);
		
		if(type.get().equals(MinecraftServer.SERVER_TYPE))
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
					StartUpApplication.addServerIPAddress(id, nodeName.get());
					response.sendRedirect(Endpoints.INDEX.get().getURL());
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
