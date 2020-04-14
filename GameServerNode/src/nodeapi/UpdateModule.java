package nodeapi;

import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Query;
import models.GameModuleTable;
import nodemain.StartUpApplication;
import server.GameServerModuleLoader;
import utils.Utils;
import utils.servlet.Endpoint;
import utils.servlet.HttpStatus;
import utils.servlet.ParameterURL;

@WebServlet("/ModuleUpdate")
public class UpdateModule extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/ModuleUpdate";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
			Endpoint.Protocol.HTTP, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL postEndpoint(String moduleName)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.MODULE_NAME.getName(), moduleName);
		return url;
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var moduleName = ApiSettings.MODULE_NAME.parse(request);
		if(!Utils.optionalsPresent(moduleName))
		{
			response.setStatus(HttpStatus.BAD_REQUEST.getCode());
			return;
		}
		
		byte[] jarBytes;
		
		try
		{
			var query = Query.query(StartUpApplication.database, GameModuleTable.class)
							 .filter(GameModuleTable.NAME, moduleName.get())
							 .first();
			
			if(query.isEmpty())
			{
				StartUpApplication.LOGGER.error(String.format("Module update failed to find module '%s' in database", moduleName.get()));
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
				return;
			}
			
			jarBytes = (byte[]) query.get().getColumnValue(GameModuleTable.JAR.getName());
		}
		catch(SQLException e)
		{
			StartUpApplication.LOGGER.error(String.format("Module update failed:\n%s", e.getMessage()));
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
			return;
		}
		
		var module = GameServerModuleLoader.loadModule(jarBytes);
		if(module == null)
		{
			StartUpApplication.LOGGER.error(String.format("Module update failed to load module '%s'", moduleName.get()));
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
			return;
		}
		
		var affectedServers = StartUpApplication.getServers()
												.entrySet()
												.stream()
												.filter(e -> e.getValue().getGameServerOptions().getServerType().equals(moduleName.get()))
												.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
		
		for(var entry : affectedServers.entrySet())
		{
			var id = entry.getKey();
			var server = entry.getValue();
			var wasRunning = server.isRunning();
			if(wasRunning)
			{
				if(!server.stopServer())
				{
					server.forceStopServer();
				}
			}
			
			var newServer = module.createGameServer(server.getFolderLocation(), server.getExecutableFile());
			StartUpApplication.addServer(id, newServer);
			if(wasRunning)
			{
				newServer.startServer();
			}
		}
	}
}
