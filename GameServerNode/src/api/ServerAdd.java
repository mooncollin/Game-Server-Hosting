package api;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import api.minecraft.MinecraftServer;
import main.NodeProperties;
import main.StartUpApplication;
import models.GameServerTable;
import models.MinecraftServerTable;
import server.GameServerFactory;

@WebServlet("/ServerAdd")
@MultipartConfig
public class ServerAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
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
		
		var foundServer = StartUpApplication.getServer(serverName);
		if(foundServer != null)
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
			StartUpApplication.addServer(generatedServer);
			
			for(var p : request.getParts())
			{
				var header = p.getHeader("Content-Disposition");
				var fileName = header.substring(header.indexOf("filename=") + "filename=".length() + 1);
				fileName = fileName.substring(0, fileName.length() - 1);
				if(fileName.endsWith(".zip"))
				{
					FileUpload.uploadFolder(Paths.get(NodeProperties.DEPLOY_FOLDER, serverName).toFile(), p.getInputStream());
				}
			}
		}
		else
		{
			response.setStatus(400);
			return;
		}
	}
}
