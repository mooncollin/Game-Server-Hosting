package api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import api.minecraft.MinecraftServer;
import main.NodeProperties;
import main.StartUpApplication;
import model.Model;
import server.GameServer;

@WebServlet("/ServerEdit")
public class ServerEdit extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
		String execName = request.getParameter("execName");
		if(serverName == null || execName == null)
		{
			response.setStatus(400);
			return;
		}
		
		GameServer foundServer = StartUpApplication.getServer(serverName);
		
		if(foundServer == null)
		{
			response.setStatus(404);
			return;
		}
		
		List<models.GameServer> serverModel = Model.getAll(models.GameServer.class, "name=?", serverName);
		if(serverModel.isEmpty())
		{
			response.setStatus(404);
			return;
		}
		
		models.GameServer foundServerModel = serverModel.get(0);
		foundServerModel.setExecutableName(execName);
		
		if(foundServer.getClass().equals(MinecraftServer.class))
		{
			String ramAmountStr = request.getParameter("ramAmount");
			int ramAmount;
			
			if(ramAmountStr == null)
			{
				response.setStatus(400);
				return;
			}
			
			try
			{
				ramAmount = Integer.valueOf(ramAmountStr);
			}
			catch(NumberFormatException e)
			{
				response.setStatus(400);
				return;
			}
			
			if(ramAmount < 2048 || ramAmount % 1024 != 0)
			{
				response.setStatus(404);
				return;
			}
			
			List<models.MinecraftServer> minecraftModel = Model.getAll(models.MinecraftServer.class, "id=?", foundServerModel.getSpecificID());
			if(minecraftModel.isEmpty())
			{
				response.setStatus(404);
				return;
			}
			
			models.MinecraftServer foundMinecraftServerModel = minecraftModel.get(0);
			foundMinecraftServerModel.setMaxHeapSize(ramAmount);
			if(foundMinecraftServerModel.commit())
			{
				((MinecraftServer) foundServer).setMaximumHeapSize(ramAmount);
			}
		}
		else
		{
			response.setStatus(404);
			return;
		}
		
		if(foundServerModel.commit())
		{
			Path folderLocation = Paths.get(NodeProperties.DEPLOY_FOLDER, serverName);
			File executableFile = folderLocation.resolve(execName).toFile();
			foundServer.setExecutableName(executableFile);
		}
	}
}
