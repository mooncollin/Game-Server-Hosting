package api;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import api.minecraft.MinecraftServer;
import main.NodeProperties;
import main.StartUpApplication;
import model.Query;
import models.GameServerTable;
import models.MinecraftServerTable;

@WebServlet("/ServerEdit")
public class ServerEdit extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverName = request.getParameter("name");
		var execName = request.getParameter("execName");
		if(serverName == null || execName == null)
		{
			response.setStatus(400);
			return;
		}
		
		var foundServer = StartUpApplication.getServer(serverName);
		
		if(foundServer == null)
		{
			response.setStatus(404);
			return;
		}
		
		try
		{
			var gameServer = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.NAME.cloneWithValue(serverName))
								  .first();
			
			gameServer.getColumn(GameServerTable.EXECUTABLE_NAME).setValue(execName);
			gameServer.commit(StartUpApplication.database);
			
			if(foundServer.getClass().equals(MinecraftServer.class))
			{
				var ramAmountStr = request.getParameter("ramAmount");
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
				
				if(ramAmount < MinecraftServer.MINIMUM_HEAP_SIZE || ramAmount % 1024 != 0)
				{
					response.setStatus(404);
					return;
				}
				
				var minecraftServer = Query.query(StartUpApplication.database, MinecraftServerTable.class)
										   .filter(MinecraftServerTable.ID.cloneWithValue(gameServer.getColumnValue(GameServerTable.SPECIFIC_ID)))
										   .first();

				minecraftServer.getColumn(MinecraftServerTable.MAX_HEAP_SIZE).setValue(ramAmount);
				minecraftServer.commit(StartUpApplication.database);
				((MinecraftServer) foundServer).setMaximumHeapSize(ramAmount);
			}
			else
			{
				response.setStatus(404);
				return;
			}
			
			var folderLocation = Paths.get(NodeProperties.DEPLOY_FOLDER, serverName);
			var executableFile = folderLocation.resolve(execName).toFile();
			foundServer.setExecutableName(executableFile);
		}
		catch(SQLException e)
		{
			response.setStatus(500);
			return;
		}
	}
}
