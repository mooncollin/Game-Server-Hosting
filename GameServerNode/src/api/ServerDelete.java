package api;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.StartUpApplication;
import model.Query;
import models.GameServerTable;
import models.MinecraftServerTable;
import models.TriggersTable;
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
		
		try
		{
			var gameServer = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.NAME.cloneWithValue(name))
								  .first();
			
			if(gameServer.getColumn(GameServerTable.SERVER_TYPE).getValue().equals("minecraft"))
			{
				var minecraftServer = Query.query(StartUpApplication.database, MinecraftServerTable.class)
										   .filter(MinecraftServerTable.ID.cloneWithValue(gameServer.getColumnValue(GameServerTable.SPECIFIC_ID)))
										   .first();
				
				minecraftServer.delete(StartUpApplication.database);
				
				var triggers = Query.query(StartUpApplication.database, TriggersTable.class)
									.filter(TriggersTable.SERVER_OWNER.cloneWithValue(gameServer.getColumnValue(GameServerTable.NAME)))
									.all();
				
				for(var trigger : triggers)
				{
					trigger.delete(StartUpApplication.database);
				}
			}
			
			gameServer.delete(StartUpApplication.database);
			
			FileDelete.deleteDirectoryOrFile(serverFound.getFolderLocation());
			StartUpApplication.removeServer(serverFound);
		}
		catch(SQLException e)
		{
			response.setStatus(500);
			return;
		}
	}
}
