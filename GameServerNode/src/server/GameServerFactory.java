package server;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import api.minecraft.MinecraftServer;
import main.NodeProperties;
import model.Model;

public class GameServerFactory
{
	public static GameServer getSpecificServer(models.GameServer server)
	{
		Objects.requireNonNull(server);
		GameServer specificServer = null;
		File folderLocation = new File(Paths.get(NodeProperties.DEPLOY_FOLDER, server.getName()).toString());
		File fileName = Paths.get(folderLocation.getAbsolutePath(), server.getExecutableName()).toFile();
		if(server.getServerType().equals("minecraft"))
		{
			List<models.MinecraftServer> minecraftServers = Model.getAll(models.MinecraftServer.class, "id=?", server.getSpecificID());
			if(minecraftServers.isEmpty())
			{
				return null;
			}
			models.MinecraftServer minecraftServer = minecraftServers.get(0);
			
			if(!folderLocation.exists())
			{
				folderLocation.mkdir();
			}
			
			specificServer = new MinecraftServer(server.getName(), folderLocation, fileName, minecraftServer.getMaxHeapSize(), minecraftServer.getArguments());
			((MinecraftServer) specificServer).autoRestart(minecraftServer.getRestarts());
		}
		
		return specificServer;
	}
}
