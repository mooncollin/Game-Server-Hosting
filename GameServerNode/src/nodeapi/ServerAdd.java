package nodeapi;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Query;
import model.Table;
import models.GameServerTable;
import models.MinecraftServerTable;
import nodemain.NodeProperties;
import nodemain.StartUpApplication;
import server.GameServerFactory;
import utils.Utils;
import utils.servlet.Endpoint;
import utils.servlet.ParameterURL;

@WebServlet("/ServerAdd")
@MultipartConfig
public class ServerAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/ServerAdd";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
			Endpoint.Protocol.HTTP, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL postEndpoint(String serverName, String execName, String type)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_NAME.getName(), serverName);
		url.addQuery(ApiSettings.EXECUTABLE_NAME.getName(), execName);
		url.addQuery(ApiSettings.SERVER_TYPE.getName(), type);
		return url;
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverName = ApiSettings.SERVER_NAME.parse(request);
		var execName = ApiSettings.EXECUTABLE_NAME.parse(request);
		var type = ApiSettings.SERVER_TYPE.parse(request);
		if(!Utils.optionalsPresent(serverName, execName, type))
		{
			response.setStatus(400);
			return;
		}
		
		Optional<Table> serverExists;
		try
		{
			serverExists = Query.query(StartUpApplication.database, GameServerTable.class)
									.filter(GameServerTable.NAME.cloneWithValue(serverName.get()))
									.first();
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.error(e.getMessage());
			response.setStatus(500);
			return;
		}
		
		if(serverExists.isPresent())
		{
			response.setStatus(400);
			return;
		}
		
		if(type.get().equals("minecraft"))
		{
			var ram = ApiSettings.RAM_AMOUNT.parse(request);
			var restart = ApiSettings.RESTARTS_UNEXPECTED.parse(request);
			if(!Utils.optionalsPresent(ram, restart))
			{
				response.setStatus(400);
				return;
			}
			
			var gameServer = new GameServerTable();
			gameServer.setColumnValue(GameServerTable.NAME, serverName.get());
			gameServer.setColumnValue(GameServerTable.NODE_OWNER, NodeProperties.NAME);
			gameServer.setColumnValue(GameServerTable.SERVER_TYPE, "minecraft");
			gameServer.setColumnValue(GameServerTable.EXECUTABLE_NAME, execName.get());
			
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
			minecraft.setColumnValue(MinecraftServerTable.MAX_HEAP_SIZE, ram.get());
			minecraft.setColumnValue(MinecraftServerTable.AUTO_RESTARTS, restart.get());
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
			StartUpApplication.addServer(gameServer.getColumnValue(GameServerTable.ID), generatedServer);
			
			var fileParts = request.getParts()
								   .parallelStream()
								   .filter(p -> p.getSubmittedFileName() != null)
								   .collect(Collectors.toList());
			
			for(var p : fileParts)
			{
				var fileName = p.getSubmittedFileName();
				if(fileName.endsWith(".zip"))
				{
					FileUpload.uploadFolder(Paths.get(NodeProperties.DEPLOY_FOLDER, serverName.get()).toFile(), p.getInputStream());
				}
			}
			
			response.getWriter().print(gameServer.getColumnValue(GameServerTable.ID));
		}
		else
		{
			response.setStatus(400);
			return;
		}
	}
}
