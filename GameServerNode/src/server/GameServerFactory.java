package server;

import java.io.File;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Objects;

import api.minecraft.MinecraftServer;
import main.NodeProperties;
import main.StartUpApplication;
import model.Query;
import model.Table;
import model.Filter.FilterType;
import models.GameServerTable;
import models.MinecraftServerTable;

public class GameServerFactory
{
	public static GameServer getSpecificServer(Table server)
	{
		Objects.requireNonNull(server);
		GameServer specificServer = null;
		var folderLocation = new File(Paths.get(NodeProperties.DEPLOY_FOLDER, server.getColumnValue(GameServerTable.NAME)).toString());
		var fileName = Paths.get(folderLocation.getAbsolutePath(), server.getColumnValue(GameServerTable.EXECUTABLE_NAME)).toFile();
		if(server.getColumn(GameServerTable.SERVER_TYPE).getValue().equals("minecraft"))
		{
			Table minecraftServer;
			try
			{
				var option = Query.query(StartUpApplication.database, MinecraftServerTable.class)
										   .join(server, GameServerTable.ID, FilterType.EQUAL, new MinecraftServerTable(), MinecraftServerTable.SERVER_ID)
										   .first();
				if(option.isEmpty())
				{
					return null;
				}
				else
				{
					minecraftServer = option.get();
				}
			} catch (SQLException e)
			{
				return null;
			}
			
			if(!folderLocation.exists())
			{
				folderLocation.mkdir();
			}
			
			specificServer = new MinecraftServer(folderLocation, fileName, minecraftServer.getColumnValue(MinecraftServerTable.MAX_HEAP_SIZE), 
					minecraftServer.getColumnValue(MinecraftServerTable.ARGUMENTS));
			
			((MinecraftServer) specificServer).autoRestart(minecraftServer.getColumnValue(MinecraftServerTable.AUTO_RESTARTS));
		}
		
		return specificServer;
	}
}
