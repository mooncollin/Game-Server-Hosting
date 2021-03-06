package nodeapi;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Query;
import model.Table;
import models.GameServerTable;
import nodemain.StartUpApplication;
import utils.Utils;

@WebServlet(
		name = "ServerEdit",
		urlPatterns = "/ServerEdit",
		asyncSupported = true
)
public class ServerEdit extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		if(!Utils.optionalsPresent(serverID))
		{
			response.setStatus(400);
			return;
		}
		
		var foundServer = StartUpApplication.getServer(serverID.get());
		
		if(foundServer == null)
		{
			response.setStatus(404);
			return;
		}
		
		Table gameServer;
		
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.ID, serverID.get())
								  .first();
			
			if(option.isEmpty())
			{
				response.setStatus(400);
				return;
			}
			
			gameServer = option.get();
		}
		catch(SQLException e)
		{
			response.setStatus(500);
			return;
		}
		
		foundServer.setExecutableFile(StartUpApplication.getExecutableFile(gameServer));
	}
}
