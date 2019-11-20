package frontend;

import java.io.ByteArrayOutputStream;
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
import server.GameServer;
import utils.Pair;

@WebServlet("/GameServerAdd")
@MultipartConfig
public class GameServerAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerAdd";
	
	private static final Pattern SERVER_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_  ]+");
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		final List<Table> nodes;
		final Map<String, Long> ramAmounts;
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
		
		final var template = new frontend.templates.GameServerAddTemplate(
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
		final var serverName = request.getParameter("name");
		final var executableName = request.getParameter("execName");
		final var nodeName = request.getParameter("node");
		final var type = request.getParameter("type");
		
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
		
		String serverAddress = null;
		for(int i = 0; i < StartUpApplication.NODE_NAMES.length; i++)
		{
			if(nodeName.equals(StartUpApplication.NODE_NAMES[i]))
			{
				serverAddress = String.format("%s:%s/%s", StartUpApplication.NODE_ADDRESSES[i], StartUpApplication.NODE_PORTS[i], ControllerProperties.NODE_EXTENSION);
				break;
			}
		}
		
		if(serverAddress == null)
		{
			doGet(request, response);
			return;
		}
		
		try
		{
			final var gameServer = Query.query(StartUpApplication.database, GameServerTable.class)
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
		
		var url = String.format("http://%s/ServerAdd?name=%s&execName=%s&type=%s", serverAddress, serverName, executableName, type);
		
		if(type.equals("minecraft"))
		{
			final var ramStr = request.getParameter("ramAmount");
			if(ramStr == null)
			{
				doGet(request, response);
				return;
			}
			
			int ram;
			try
			{
				ram = Integer.parseInt(ramStr);
			}
			catch(NumberFormatException e)
			{
				doGet(request, response);
				return;
			}
			
			if(ram < MinecraftServer.MINIMUM_HEAP_SIZE || ram % 1024 != 0)
			{
				doGet(request, response);
				return;
			}
			
			final var restart = request.getParameter("restartsUnexpected") == null ? "no" : "yes";
			
			url += String.format("&ram=%s&restart=%s", ramStr, restart);
		}
		
		final var boundary = "===" + System.currentTimeMillis() + "===";
		
		var zipRequest = new ByteArrayOutputStream();
		for(final var p : request.getParts())
		{
			final var header = p.getHeader("Content-Disposition");
			var fileName = header.substring(header.indexOf("filename=") + "filename=".length() + 1);
			fileName = fileName.substring(0, fileName.length() - 1);
			if(fileName.endsWith(".zip"))
			{
				zipRequest.writeBytes(String.format("\r\n--%s\r\nContent-Disposition: %s\r\n\r\n", boundary, p.getHeader("Content-Disposition")).getBytes());
				p.getInputStream().transferTo(zipRequest);
			}
		}
		
		zipRequest.writeBytes(String.format("\r\n--%s--", boundary).getBytes());
		url = url.replace(" ", "+");
		final var httpRequest = HttpRequest.newBuilder(URI.create(url))
				.header("Content-type", "multipart/form-data; boundary=" + boundary)
				.POST(BodyPublishers.ofByteArray(zipRequest.toByteArray()))
				.build();
		zipRequest.reset();
		zipRequest.close();
		try
		{
			final var httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
			if(httpResponse.statusCode() == 200)
			{
				StartUpApplication.getServerInfo().put(serverName, new Pair<Class<? extends GameServer>, String>(MinecraftServer.class, serverAddress));
				response.sendRedirect(Index.URL);
				return;
			}
		}
		catch(InterruptedException e)
		{
		}
		
		doGet(request, response);
	}
}
