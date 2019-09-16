package api;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.StartUpApplication;
import model.Model;
import server.GameServer;

@WebServlet("/ServerDelete")
public class ServerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String name = request.getParameter("name");
		
		if(name == null)
		{
			response.setStatus(400);
			return;
		}
		
		GameServer serverFound = StartUpApplication.getServer(name);
		if(serverFound == null)
		{
			response.setStatus(400);
			return;
		}
		
		List<models.GameServer> gameServers = Model.getAll(models.GameServer.class, "name=?", name);
		if(gameServers.isEmpty())
		{
			response.setStatus(400);
			return;
		}
		
		models.GameServer gameServer = gameServers.get(0);
		
		if(gameServer.getServerType().equals("minecraft"))
		{
			List<models.MinecraftServer> minecraftServers = Model.getAll(models.MinecraftServer.class, "id=?", gameServer.getSpecificID());
			if(minecraftServers.isEmpty())
			{
				response.setStatus(400);
				return;
			}
			
			models.MinecraftServer minecraftServer = minecraftServers.get(0);
			if(!minecraftServer.delete())
			{
				response.setStatus(400);
				return;
			}
			
			List<models.Triggers> triggers = Model.getAll(models.Triggers.class, "serverowner=?", gameServer.getName());
			for(models.Triggers trigger : triggers)
			{
				trigger.delete();
			}
		}
		
		if(!gameServer.delete())
		{
			response.setStatus(400);
			return;
		}
		
		FileDelete.deleteDirectoryOrFile(serverFound.getFolderLocation());
		StartUpApplication.removeServer(serverFound);
	}
}
