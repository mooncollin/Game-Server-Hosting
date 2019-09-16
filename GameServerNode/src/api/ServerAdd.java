package api;

import java.io.IOException;
import java.nio.file.Paths;

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
			
			models.MinecraftServer minecraft = new models.MinecraftServer(ram, restart.equals("yes"), "");
			if(!minecraft.commit())
			{
				response.setStatus(400);
				return;
			}
			
			models.GameServer gameServer = new models.GameServer(serverName, NodeProperties.NAME, minecraft.getID(), "minecraft", execName);
			if(!gameServer.commit())
			{
				response.setStatus(400);
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
