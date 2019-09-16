package api.minecraft;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Model;
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
			String serverCommand = request.getParameter("serverCommand");
			if(serverCommand == null)
			{
				return false;
			}
			
			server.writeToServer(serverCommand);
		}
		else if(command.equals("properties"))
		{
			Properties currentProperties = ((MinecraftServer) server).getProperties();
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
		
		List<models.GameServer> gameServers = Model.getAll(models.GameServer.class, "name=?", server.getName());
		if(gameServers.isEmpty())
		{
			return false;
		}
		models.GameServer gameServer = gameServers.get(0);
		
		List<models.MinecraftServer> minecraftServers = Model.getAll(models.MinecraftServer.class, "id=?", gameServer.getSpecificID());
		if(minecraftServers.isEmpty())
		{
			return false;
		}
		models.MinecraftServer minecraftServer = minecraftServers.get(0);
		
		if(command.equals("properties"))
		{
			MinecraftServer minecraft = (MinecraftServer) server;
			Properties props = new Properties();
			for(String key : MinecraftServer.MINECRAFT_PROPERTIES.keySet())
			{
				String property = request.getParameter(key);
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
			MinecraftServer minecraft = (MinecraftServer) server;
			String restarts = request.getParameter("restartsUnexpected");;
			minecraft.autoRestart(restarts != null);
			
			
			if(minecraftServers.isEmpty())
			{
				return false;
			}
			
			minecraftServer.setRestarts(minecraft.autoRestart());
			
		}
		else if(command.equals("arguments"))
		{
			MinecraftServer minecraft = (MinecraftServer) server;
			String arguments = request.getParameter("arguments");
			if(arguments == null)
			{
				return false;
			}
			minecraftServer.setArguments(arguments);
			minecraft.setArguments(arguments);
		}
		else
		{
			return false;
		}
		
		minecraftServer.commit();
		return true;
	}
}