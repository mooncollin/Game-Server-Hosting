package server;

import java.io.File;

import model.Database;

public interface GameServerModule
{
	public GameServerUIHandler gameServerUIHandler();
	public GameServerOptions<?> gameServerOptions();
	public GameServerCommandHandler<?> gameServerCommandHandler();
	public GameServer createGameServer(File folderLocation, File fileName);
	public void setup(Database db);
}
