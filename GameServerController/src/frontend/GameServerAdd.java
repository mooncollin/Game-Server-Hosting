package frontend;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import api.minecraft.MinecraftServer;
import backend.api.ServerInteract;
import backend.main.ControllerProperties;
import backend.main.StartUpApplication;
import model.Query;
import model.Table;
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
		url.addQuery(ApiSettings.SERVER_NAME_PARAMETER, serverName);
		url.addQuery(ApiSettings.EXECUTABLE_NAME_PARAMETER, execName);
		url.addQuery(ApiSettings.NODE_NAME_PARAMETER, nodeName);
		url.addQuery(ApiSettings.SERVER_TYPE_PARAMETER, type);
		
		return url;
	}
	
	private static final Pattern SERVER_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_  ]+");
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		List<Table> nodes;
		Map<String, Long> ramAmounts;
		try
		{
			nodes = Query.query(StartUpApplication.database, NodeTable.class)
							 .all();
			
			ramAmounts = nodes.stream()
							  .map(node -> node.getColumnValue(NodeTable.NAME))
							  .collect(Collectors.toMap(nodeName -> nodeName, 
									  nodeName -> {
										try
										{
											return StartUpApplication.getNodeReservedRam(nodeName);
										} catch (SQLException e)
										{
											throw new RuntimeException(e.getMessage());
										}
									}));
			
		} catch (SQLException | RuntimeException e1)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e1.getMessage());
			response.setStatus(500);
			return;
		}
		
		var template = new frontend.templates.GameServerAddTemplate(
				SERVER_NAME_PATTERN,
				ControllerProperties.NODE_NAMES.split(","),
				nodes,
				ramAmounts,
				GameServer.PROPERTY_NAMES_TO_TYPE.keySet());
		
		response.setContentType("text/html");
		response.getWriter().print(template);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverName = request.getParameter(ApiSettings.SERVER_NAME_PARAMETER);
		var executableName = request.getParameter(ApiSettings.EXECUTABLE_NAME_PARAMETER);
		var nodeName = request.getParameter(ApiSettings.NODE_NAME_PARAMETER);
		var type = request.getParameter(ApiSettings.SERVER_TYPE_PARAMETER);
		
		if(serverName == null || executableName == null || nodeName == null || type == null)
		{
			doGet(request, response);
			return;
		}
		
		if(!SERVER_NAME_PATTERN.matcher(serverName).matches())
		{
			doGet(request, response);
			return;
		}
		
		var serverAddress = StartUpApplication.nodeIPAddresses.get(nodeName);
		
		if(serverAddress == null)
		{
			doGet(request, response);
			return;
		}
		
		try
		{
			var gameServer = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.NAME.cloneWithValue(serverName))
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
		
		var url = nodeapi.ServerAdd.postEndpoint(serverName, executableName, type);
		url.setHost(serverAddress);
		
		if(type.equals("minecraft"))
		{
			var ram = Utils.fromString(Long.class, request.getParameter("ramAmount"));
			if(ram == null)
			{
				doGet(request, response);
				return;
			}
			
			if(ram < MinecraftServer.MINIMUM_HEAP_SIZE || ram % 1024 != 0)
			{
				doGet(request, response);
				return;
			}
			
			var restart = request.getParameter("restartsUnexpected") == null ? "no" : "yes";
			
			url.addQuery("ram", ram);
			url.addQuery("restart", restart);
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
				var httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.ofString());
				if(httpResponse.statusCode() == 200)
				{
					var id = Integer.parseInt(httpResponse.body());
					StartUpApplication.serverTypes.put(id, GameServer.PROPERTY_NAMES_TO_TYPE.get(type));
					StartUpApplication.serverIPAddresses.put(id, serverAddress);
					response.sendRedirect(Index.URL);
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
