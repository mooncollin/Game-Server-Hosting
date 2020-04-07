package api.minecraft;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Query;
import model.Table;
import model.Filter.FilterType;
import models.GameServerTable;
import models.MinecraftServerTable;
import nodeapi.ApiSettings;
import nodemain.StartUpApplication;
import server.GameServerCommandHandler;

public class MinecraftServerCommandHandler extends GameServerCommandHandler<MinecraftServer>
{
	public static final String LAST_COMMAND = "last";
	public static final String SERVER_COMMAND_COMMAND = "serverCommand";
	public static final String PROPERTIES_COMMAND = "properties";
	public static final String RESTARTS_COMMAND = "restarts";
	
	public static final List<String[]> COMMANDS;
	
	static
	{
		var temp = new LinkedList<String[]>();
		temp.add(new String[]{SERVER_COMMAND_COMMAND, SERVER_COMMAND_COMMAND});
		temp.add(new String[]{LAST_COMMAND});
		temp.add(new String[]{PROPERTIES_COMMAND});
		temp.add(new String[]{RESTARTS_COMMAND});
		temp.addAll(GameServerCommandHandler.COMMANDS);
		COMMANDS = Collections.unmodifiableList(temp);
	}
	
	public MinecraftServerCommandHandler(MinecraftServer server)
	{
		super(server);
		commands = new LinkedList<String[]>(COMMANDS);
	}
	
	@Override
	public boolean commandGET(String command, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		if(super.commandGET(command, request, response))
		{
			return true;
		}
		
		if(command.equals(LAST_COMMAND))
		{
			var lastRead = server.getLastRead();
			String[] lastReadCopy;
			synchronized(lastRead)
			{
				lastReadCopy = lastRead.toArray(String[]::new);
			}
			
			Stream.of(lastReadCopy)
				  .filter(line -> line != null)
				  .forEach(response.getWriter()::print);
		}
		else if(command.equals(SERVER_COMMAND_COMMAND))
		{
			var serverCommand = request.getParameter(SERVER_COMMAND_COMMAND);
			if(serverCommand == null)
			{
				return false;
			}
			
			server.writeToServer(serverCommand);
		}
		else if(command.equals(PROPERTIES_COMMAND))
		{
			var currentProperties = server.getProperties();
			for(var name : currentProperties.stringPropertyNames())
			{
				response.getWriter().print(String.format("%s=%s\r\n", name, currentProperties.getProperty(name)));
			}
		}
		else if(command.equals(RESTARTS_COMMAND))
		{
			response.getWriter().print(server.autoRestart() ? "yes" : "no");
		}
		else
		{
			return false;
		}
		
		response.setContentType("text/plain");
		return true;
	}
	
	@Override
	public boolean commandPOST(String command, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		if(super.commandPOST(command, request, response))
		{
			return true;
		}
		
		Table minecraftServer;
		try
		{
			var gameServer = Query.query(StartUpApplication.database, GameServerTable.class)
							   	  .filter(GameServerTable.ID.cloneWithValue(StartUpApplication.getID(server)))
							   	  .first();
			
			if(gameServer.isEmpty())
			{
				return false;
			}
			
			var option = Query.query(StartUpApplication.database, MinecraftServerTable.class)
					   .join(new GameServerTable(), GameServerTable.ID, FilterType.EQUAL, new MinecraftServerTable(), MinecraftServerTable.SERVER_ID)
					   .first();
			
			if(option.isEmpty())
			{
				return false;
			}
			else
			{
				minecraftServer = option.get();
			}
			
		} catch (SQLException e)
		{
			throw new RuntimeException(e.getMessage());
		}
		
		if(command.equals(ApiSettings.PROPERTIES.getName()))
		{
			var props = new Properties();
			for(var key : MinecraftServer.MINECRAFT_PROPERTIES.keySet())
			{
				var property = request.getParameter(key);
				if(property == null)
				{
					property = String.valueOf(MinecraftServer.MINECRAFT_PROPERTIES.get(key));
				}
				if(MinecraftServer.MINECRAFT_PROPERTIES.get(key) instanceof Boolean)
				{
					property = String.valueOf(property.equals("on"));
				}
				props.setProperty(key, property);
			}
			server.setProperties(props);
		}
		else
		{
			return false;
		}
		
		try
		{
			minecraftServer.commit(StartUpApplication.database);
		} catch (SQLException e)
		{
			throw new RuntimeException(e.getMessage());
		}
		
		return true;
	}
}