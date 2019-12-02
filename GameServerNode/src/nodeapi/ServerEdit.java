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
import model.Filter.FilterType;
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
	
	public static ParameterURL postEndpoint(int id, String execName)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID_PARAMETER, id);
		url.addQuery(ApiSettings.EXECUTABLE_NAME_PARAMETER, execName);
		return url;
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		var execName = request.getParameter(ApiSettings.EXECUTABLE_NAME_PARAMETER);
		if(serverID == null || execName == null)
		{
			response.setStatus(400);
			return;
		}
		
		var foundServer = StartUpApplication.getServer(serverID);
		
		if(foundServer == null)
		{
			response.setStatus(404);
			return;
		}
		
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.ID.cloneWithValue(serverID))
								  .first();
			
			Table gameServer;
			
			if(option.isEmpty())
			{
				response.setStatus(400);
				return;
			}
			else
			{
				gameServer = option.get();
			}
			
			gameServer.getColumn(GameServerTable.EXECUTABLE_NAME).setValue(execName);
			gameServer.commit(StartUpApplication.database);
			
			if(foundServer.getClass().equals(MinecraftServer.class))
			{
				var ramAmount = Utils.fromString(Integer.class, request.getParameter("ramAmount"));
				
				if(ramAmount == null)
				{
					response.setStatus(400);
					return;
				}
				
				
				if(ramAmount < MinecraftServer.MINIMUM_HEAP_SIZE || ramAmount % 1024 != 0)
				{
					response.setStatus(400);
					return;
				}
				
				var option2 = Query.query(StartUpApplication.database, MinecraftServerTable.class)
										   .join(gameServer, GameServerTable.ID, FilterType.EQUAL, new MinecraftServerTable(), MinecraftServerTable.ID)
										   .first();
				
				Table minecraftServer;
				
				if(option2.isEmpty())
				{
					response.setStatus(400);
					return;
				}
				else
				{
					minecraftServer = option2.get();
				}

				minecraftServer.setColumnValue(MinecraftServerTable.MAX_HEAP_SIZE, ramAmount);
				minecraftServer.commit(StartUpApplication.database);
				((MinecraftServer) foundServer).setMaximumHeapSize(ramAmount);
			}
			else
			{
				response.setStatus(400);
				return;
			}
			
			var folderLocation = Paths.get(NodeProperties.DEPLOY_FOLDER, gameServer.getColumnValue(GameServerTable.NAME));
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
