package server;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

import model.Query;
import model.Table;
import models.GameModuleTable;
import models.GameServerTable;
import nodemain.StartUpApplication;

public class GameServerFactory
{
	synchronized public static GameServer getSpecificServer(Table server)
	{
		Objects.requireNonNull(server);
		GameServer specificServer = null;
		var folderLocation = StartUpApplication.getServerFolder(server);
		var executableFile = StartUpApplication.getExecutableFile(server);
		
		var module = StartUpApplication.getModule(server.getColumnValue(GameServerTable.SERVER_TYPE));
		if(module == null)
		{
			try
			{
				var query = Query.query(StartUpApplication.database, GameModuleTable.class)
								 .filter(GameModuleTable.NAME, server.getColumnValue(GameServerTable.SERVER_TYPE))
								 .first();
				
				if(query.isEmpty())
				{
					StartUpApplication.LOGGER.error(String.format("Game module: '%s' does not exist", server.getColumnValue(GameServerTable.SERVER_TYPE)));
					return null;
				}
				
				var jarBytes = (byte[]) query.get().getColumnValue(GameModuleTable.JAR.getName());
				module = GameServerModuleLoader.loadModule(jarBytes);
			}
			catch(SQLException e)
			{
				StartUpApplication.LOGGER.error(String.format("Error fetching game modules from database:\n%s", e.getMessage()));
				return null;
			} catch (IOException e)
			{
				StartUpApplication.LOGGER.error(String.format("Error loading game module from database:\n%s", e.getMessage()));
				return null;
			}
		}
		
		if(module != null)
		{
			StartUpApplication.addModule(module);
			specificServer = module.createGameServer(folderLocation, executableFile);
			if(!folderLocation.exists())
			{
				folderLocation.mkdir();
			}
		}
		else
		{
			try
			{
				server.delete(StartUpApplication.database);
			} catch (SQLException e)
			{
				StartUpApplication.LOGGER.error(String.format("Error deleting server from database:\n%s", e.getMessage()));
			}
		}
		
		return specificServer;
	}
}
