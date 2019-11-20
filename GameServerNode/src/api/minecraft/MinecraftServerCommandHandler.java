package api.minecraft;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.StartUpApplication;
import model.Query;
import model.Table;
import model.Filter.FilterType;
import models.GameServerTable;
import models.MinecraftServerTable;
import server.GameServer;
import server.GameServerCommandHandler;

public class MinecraftServerCommandHandler extends GameServerCommandHandler
{
	public static final List<String[]> COMMANDS;
	
	static
	{
		var temp = new LinkedList<String[]>();
		temp.add(new String[]{"serverCommand", "serverCommand"});
		temp.add(new String[]{"last"});
		temp.add(new String[]{"properties"});
		temp.add(new String[]{"restarts"});
		temp.addAll(GameServerCommandHandler.COMMANDS);
		COMMANDS = Collections.unmodifiableList(temp);
	}
	
	public MinecraftServerCommandHandler(GameServer server)
	{
		super(server);
		assert(server instanceof MinecraftServer);
		commands = new LinkedList<String[]>(COMMANDS);
	}
	
	@Override
	public boolean commandGET(String command, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		if(super.commandGET(command, request, response))
		{
			return true;
		}
		
		if(command.equals("last"))
		{
			var lastRead = ((MinecraftServer) server).getLastRead();
			String[] lastReadCopy;
			synchronized(lastRead)
			{
				lastReadCopy = lastRead.toArray(new String[] {});
			}
			
			for(String line : lastReadCopy)
			{
				if(line != null)
				{
					response.getWriter().print(line);
				}
			}
		}
		else if(command.equals("serverCommand"))
		{
			var serverCommand = request.getParameter("serverCommand");
			if(serverCommand == null)
			{
				return false;
			}
			
			server.writeToServer(serverCommand);
		}
		else if(command.equals("properties"))
		{
			var currentProperties = ((MinecraftServer) server).getProperties();
			for(String name : currentProperties.stringPropertyNames())
			{
				response.getWriter().println(String.format("%s=%s", name, currentProperties.getProperty(name)));
			}
		}
		else if(command.equals("restarts"))
		{
			response.getWriter().print(((MinecraftServer) server).autoRestart() ? "yes" : "no");
		}
		else
		{
			return false;
		}
		
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
							   	  .filter(GameServerTable.NAME.cloneWithValue(server.getName()))
							   	  .first();
			
			if(gameServer == null)
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
		
		if(command.equals("properties"))
		{
			var minecraft = (MinecraftServer) server;
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
			minecraft.setProperties(props);
		}
		else if(command.equals("restarts"))
		{
			var minecraft = (MinecraftServer) server;
			var restarts = request.getParameter("restartsUnexpected");
			minecraft.autoRestart(restarts != null);
			minecraftServer.setColumnValue(MinecraftServerTable.AUTO_RESTARTS, minecraft.autoRestart());
		}
		else if(command.equals("arguments"))
		{
			var minecraft = (MinecraftServer) server;
			var arguments = request.getParameter("arguments");
			if(arguments == null)
			{
				return false;
			}
			
			minecraftServer.setColumnValue(MinecraftServerTable.ARGUMENTS, arguments);
			minecraft.setArguments(arguments);
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