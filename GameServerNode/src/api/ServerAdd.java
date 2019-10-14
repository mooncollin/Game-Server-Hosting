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
import javax.servlet.http.Part;

import api.minecraft.MinecraftServer;
import main.NodeProperties;
import main.StartUpApplication;
import models.GameServerTable;
import models.MinecraftServerTable;
import server.GameServer;
import server.GameServerFactory;

@WebServlet("/ServerAdd")
@MultipartConfig
public class ServerAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
		String execName = request.getParameter("execName");
		String type = request.getParameter("type");
		if(serverName == null || execName == null || type == null)
		{
			response.setStatus(400);
			return;
		}
		
		GameServer foundServer = StartUpApplication.getServer(serverName);
		if(foundServer != null)
		{
			response.setStatus(400);
			return;
		}
		
		if(type.equals("minecraft"))
		{
			String ramStr = request.getParameter("ram");
			int ram;
			String restart = request.getParameter("restart");
			if(ramStr == null || restart == null)
			{
				response.setStatus(400);
				return;
			}
			
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
			
			var minecraft = new MinecraftServerTable();
			minecraft.getColumn(MinecraftServerTable.MAX_HEAP_SIZE).setValue(ram);
			minecraft.getColumn(MinecraftServerTable.AUTO_RESTARTS).setValue(restart.equals("yes"));
			minecraft.getColumn(MinecraftServerTable.ARGUMENTS).setValue("");
			
			try
			{
				minecraft.commit(StartUpApplication.database);
			} catch (SQLException e1)
			{
				response.setStatus(500);
				return;
			}
			
			var gameServer = new GameServerTable();
			gameServer.getColumn(GameServerTable.NAME).setValue(serverName);
			gameServer.getColumn(GameServerTable.NODE_OWNER).setValue(NodeProperties.NAME);
			gameServer.getColumn(GameServerTable.SPECIFIC_ID).setValue(minecraft.getColumn(MinecraftServerTable.ID).getValue());
			gameServer.getColumn(GameServerTable.SERVER_TYPE).setValue("minecraft");
			gameServer.getColumn(GameServerTable.EXECUTABLE_NAME).setValue(execName);
			
			try
			{
				gameServer.commit(StartUpApplication.database);
			}
			catch(SQLException e)
			{
				response.setStatus(500);
				return;
			}
			
			GameServer generatedServer = GameServerFactory.getSpecificServer(gameServer);
			StartUpApplication.addServer(generatedServer);
			
			for(Part p : request.getParts())
			{
				String header = p.getHeader("Content-Disposition");
				String fileName = header.substring(header.indexOf("filename=") + "filename=".length() + 1);
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
