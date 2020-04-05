package nodeapi;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import api.minecraft.MinecraftServer;
import model.Query;
import model.Table;
import models.GameServerTable;
import models.MinecraftServerTable;
import nodemain.NodeProperties;
import nodemain.StartUpApplication;
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/ServerEdit")
public class ServerEdit extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/ServerEdit";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		ParameterURL.HTTP_PROTOCOL, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL postEndpoint(int id)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID.getName(), id);
		return url;
	}
	
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
		
		var foundMinecraftServer = (MinecraftServer) foundServer;
		
		Table gameServer;
		Table minecraftServer;
		
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.ID.cloneWithValue(serverID.get()))
								  .first();
			
			if(option.isEmpty())
			{
				response.setStatus(400);
				return;
			}
			
			gameServer = option.get();
			
			if(foundServer.getClass().equals(MinecraftServer.class))
			{
				var option2 = Query.query(StartUpApplication.database, MinecraftServerTable.class)
										   .filter(MinecraftServerTable.SERVER_ID, gameServer.getColumnValue(GameServerTable.ID))
										   .first();
				
				if(option2.isEmpty())
				{
					response.setStatus(400);
					return;
				}
				
				minecraftServer = option2.get();
			}
			else
			{
				response.setStatus(400);
				return;
			}
		}
		catch(SQLException e)
		{
			response.setStatus(500);
			return;
		}
		
		var folderLocation = Paths.get(NodeProperties.DEPLOY_FOLDER, gameServer.getColumnValue(GameServerTable.NAME));
		var executableFile = folderLocation.resolve(gameServer.getColumnValue(GameServerTable.EXECUTABLE_NAME)).toFile();
		foundServer.setExecutableName(executableFile);
		
		foundMinecraftServer.setMaximumHeapSize(minecraftServer.getColumnValue(MinecraftServerTable.MAX_HEAP_SIZE));
		foundMinecraftServer.setArguments(minecraftServer.getColumnValue(MinecraftServerTable.ARGUMENTS));
		foundMinecraftServer.autoRestart(minecraftServer.getColumnValue(MinecraftServerTable.AUTO_RESTARTS));
	}
}
