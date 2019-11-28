package api;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import api.minecraft.MinecraftServer;
import main.NodeProperties;
import main.StartUpApplication;
import model.Query;
import model.Table;
import models.GameServerTable;
import models.MinecraftServerTable;
import server.GameServerFactory;

@WebServlet("/ServerAdd")
@MultipartConfig
public class ServerAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/ServerAdd";
	
	public static String getEndpoint(String serverName, String execName, String type)
	{
		return String.format("%s?name=%s&execName=%s&type=%s", URL, serverName, execName, type);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverName = request.getParameter("name");
		var execName = request.getParameter("execName");
		var type = request.getParameter("type");
		if(serverName == null || execName == null || type == null)
		{
			response.setStatus(400);
			return;
		}
		
		Optional<Table> serverExists;
		try
		{
			serverExists = Query.query(StartUpApplication.database, GameServerTable.class)
									.filter(GameServerTable.NAME.cloneWithValue(serverName))
									.first();
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
		
		if(serverExists.isPresent())
		{
			response.setStatus(400);
			return;
		}
		
		if(type.equals("minecraft"))
		{
			var ramStr = request.getParameter("ram");
			var restart = request.getParameter("restart");
			if(ramStr == null || restart == null)
			{
				response.setStatus(400);
				return;
			}
			
			int ram;
			try
			{
				ram = Integer.parseInt(ramStr);
			}
			catch(NumberFormatException e)
			{
				response.setStatus(400);
				return;
			}
			
			if(ram < MinecraftServer.MINIMUM_HEAP_SIZE || ram % 1024 != 0)
			{
				response.setStatus(400);
				return;
			}
			
			var gameServer = new GameServerTable();
			gameServer.setColumnValue(GameServerTable.NAME, serverName);
			gameServer.setColumnValue(GameServerTable.NODE_OWNER, NodeProperties.NAME);
			gameServer.setColumnValue(GameServerTable.SERVER_TYPE, "minecraft");
			gameServer.setColumnValue(GameServerTable.EXECUTABLE_NAME, execName);
			
			try
			{
				gameServer.commit(StartUpApplication.database);
			}
			catch(SQLException e)
			{
				response.setStatus(500);
				return;
			}
			
			var minecraft = new MinecraftServerTable();
			minecraft.setColumnValue(MinecraftServerTable.MAX_HEAP_SIZE, ram);
			minecraft.setColumnValue(MinecraftServerTable.AUTO_RESTARTS, restart.equals("yes"));
			minecraft.setColumnValue(MinecraftServerTable.ARGUMENTS, "");
			minecraft.setColumnValue(MinecraftServerTable.SERVER_ID, gameServer.getColumnValue(GameServerTable.ID));
			
			try
			{
				minecraft.commit(StartUpApplication.database);
			} catch (SQLException e1)
			{
				response.setStatus(500);
				return;
			}
			
			var generatedServer = GameServerFactory.getSpecificServer(gameServer);
			StartUpApplication.addServer(gameServer.getColumnValue(GameServerTable.ID), generatedServer);
			
			var fileParts = request.getParts()
								   .parallelStream()
								   .filter(p -> p.getSubmittedFileName() != null)
								   .collect(Collectors.toList());
			
			for(var p : fileParts)
			{
				var fileName = p.getSubmittedFileName();
				if(fileName.endsWith(".zip"))
				{
					FileUpload.uploadFolder(Paths.get(NodeProperties.DEPLOY_FOLDER, serverName).toFile(), p.getInputStream());
				}
			}
			
			response.getWriter().print(gameServer.getColumnValue(GameServerTable.ID));
		}
		else
		{
			response.setStatus(400);
			return;
		}
	}
}
