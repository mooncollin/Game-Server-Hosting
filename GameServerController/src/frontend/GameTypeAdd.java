package frontend;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.rowset.serial.SerialBlob;

import org.apache.tomcat.util.codec.binary.Base64;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import backend.main.StartUpApplication;
import model.Query;
import models.GameModuleTable;
import server.GameServerModule;
import server.GameServerModuleLoader;
import utils.servlet.HttpStatus;

/**
 * The frontend for adding a new game type.
 * @author Collin
 *
 */
@WebServlet(
		name = "GameTypeAdd",
		urlPatterns = "/GameTypeAdd",
		asyncSupported = true
)
@MultipartConfig
public class GameTypeAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		
		var template = Velocity.getTemplate("addGameType.vm");
		template.merge(context, response.getWriter());
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var jarPart = request.getPart("jar");
		var iconPart = request.getPart("icon");
		var name = request.getParameter("name");
		
		byte[] jarBytes = null;
		byte[] iconBytes = null;
		
		if(jarPart != null && jarPart.getInputStream() != null)
		{
			jarBytes = jarPart.getInputStream().readAllBytes();
		}
		
		if(iconPart != null && iconPart.getInputStream() != null)
		{
			iconBytes = iconPart.getInputStream().readAllBytes();
		}
		
		var iconString = "";
		if(iconBytes != null && iconBytes.length > 0)
		{
			iconString = Base64.encodeBase64String(iconBytes);
		}
		
		GameServerModule module = null;
		if(jarBytes != null && jarBytes.length > 0)
		{
			module = GameServerModuleLoader.loadModule(jarBytes);
			GameServerModuleLoader.loadModuleResources(jarBytes, new File(StartUpApplication.APPLICATION_ROOT));
		}
		
		if(name != null)
		{
			try
			{
				var query = Query.query(StartUpApplication.database, GameModuleTable.class)
						 .filter(GameModuleTable.NAME, name)
						 .first();
				
				if(query.isEmpty())
				{
					StartUpApplication.LOGGER.warn("Cannot find game module in database");
					response.sendRedirect(Endpoints.GAME_TYPES.get().getURL());
					return;
				}
				
				var moduleTable = query.get();
				if(module != null)
				{
					moduleTable.setColumnValue(GameModuleTable.JAR, new SerialBlob(jarBytes));
				}
				if(iconBytes != null && iconBytes.length > 0)
				{
					moduleTable.setColumnValue(GameModuleTable.ICON, new SerialBlob(iconString.getBytes()));
				}
				
				moduleTable.commit(StartUpApplication.database);
				
				if(module != null)
				{
					StartUpApplication.addModule(module);
					var url = nodeapi.Endpoints.MODULE_UPDATE.post(name);
					for(var address : StartUpApplication.getNodeIPAddresses())
					{
						url.setHost(address);
						var httpRequest = HttpRequest.newBuilder(URI.create(url.getURL()))
													 .POST(BodyPublishers.noBody())
													 .build();
						try
						{
							var httpResponse = StartUpApplication.client.send(httpRequest, BodyHandlers.discarding());
							if(httpResponse.statusCode() != HttpStatus.OK.getCode())
							{
								StartUpApplication.LOGGER.error(String.format("Did not get %d http status from asking for node to update a game module. Instead got: %d", HttpStatus.OK.getCode(), httpResponse.statusCode()));
								response.setStatus(httpResponse.statusCode());
							}
						}
						catch(InterruptedException e)
						{
						}
					}
				}
				response.sendRedirect(Endpoints.GAME_TYPES.get().getURL());
				return;
			}
			catch(SQLException e)
			{
				StartUpApplication.LOGGER.error(String.format("Error with database:\n%s", e.getMessage()));
				response.sendRedirect(Endpoints.GAME_TYPES.get().getURL());
				return;
			}
		}
		
		if(module == null)
		{
			StartUpApplication.LOGGER.error("Module uploaded could not be dynamically loaded");
			response.sendRedirect(Endpoints.GAME_TYPE_ADD.get().getURL());
			return;
		}
		
		var moduleName = module.gameServerOptions().getServerType();

		try
		{
			var moduleTable = new GameModuleTable();
			
			moduleTable.setColumnValue(GameModuleTable.NAME, moduleName);
			moduleTable.setColumnValue(GameModuleTable.JAR, new SerialBlob(jarBytes));
			moduleTable.setColumnValue(GameModuleTable.ICON, new SerialBlob(iconString.getBytes()));
			moduleTable.commit(StartUpApplication.database);
		}
		catch(SQLException e)
		{
			StartUpApplication.LOGGER.error(String.format("Error with database:\n%s", e.getMessage()));
			response.sendRedirect(Endpoints.GAME_TYPE_ADD.get().getURL());
			return;
		}
		
		StartUpApplication.addModule(module);
		response.sendRedirect(Endpoints.GAME_TYPES.get().getURL());
	}
}
