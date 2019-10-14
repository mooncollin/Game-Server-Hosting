package server;

import java.io.File;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Objects;

import api.minecraft.MinecraftServer;
import main.NodeProperties;
import main.StartUpApplication;
import model.Query;
import model.TableTemp;
import models.GameServerTable;
import models.MinecraftServerTable;

public class GameServerFactory
{
	public static GameServer getSpecificServer(TableTemp server)
	{
		Objects.requireNonNull(server);
		GameServer specificServer = null;
		var folderLocation = new File(Paths.get(NodeProperties.DEPLOY_FOLDER, server.getColumnValue(GameServerTable.NAME)).toString());
		var fileName = Paths.get(folderLocation.getAbsolutePath(), server.getColumnValue(GameServerTable.EXECUTABLE_NAME)).toFile();
		if(server.getColumn(GameServerTable.SERVER_TYPE).getValue().equals("minecraft"))
		{
			TableTemp minecraftServer;
			try
			{
				minecraftServer = Query.query(StartUpApplication.database, MinecraftServerTable.class)
										   .filter(MinecraftServerTable.ID.cloneWithValue(server.getColumnValue(GameServerTable.SPECIFIC_ID)))
										   .first();
			} catch (SQLException e)
			{
				return null;
			}
			
			if(!folderLocation.exists())
			{
				folderLocation.mkdir();
			}
			
			specificServer = new MinecraftServer(server.getColumnValue(GameServerTable.NAME), 
					folderLocation, fileName, minecraftServer.getColumnValue(MinecraftServerTable.MAX_HEAP_SIZE), 
					minecraftServer.getColumnValue(MinecraftServerTable.ARGUMENTS));
			
			((MinecraftServer) specificServer).autoRestart(minecraftServer.getColumnValue(MinecraftServerTable.AUTO_RESTARTS));
		}
		
		return specificServer;
	}
}
