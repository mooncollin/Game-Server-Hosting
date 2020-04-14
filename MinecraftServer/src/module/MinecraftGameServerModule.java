package module;

import java.io.File;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import minecraft.server.MinecraftServer;
import minecraft.server.MinecraftServerCommandHandler;
import minecraft.server.MinecraftServerOptions;
import minecraft.server.MinecraftServerTable;
import minecraft.server.MinecraftServerUIHandler;
import model.Database;
import server.GameServerModule;

public class MinecraftGameServerModule implements GameServerModule
{
	public static final Logger LOGGER = LoggerFactory.getLogger(MinecraftGameServerModule.class);
	public static final String JAVA8;
	
	static
	{
		JAVA8 = Objects.requireNonNull(System.getenv("JAVA8"), "Do you have the JAVA8 environment variable set?");
	}
	
	public MinecraftServerUIHandler gameServerUIHandler()
	{
		return new MinecraftServerUIHandler();
	}
	
	public MinecraftServerOptions gameServerOptions()
	{
		return new MinecraftServerOptions();
	}
	
	public MinecraftServerCommandHandler gameServerCommandHandler()
	{
		return new MinecraftServerCommandHandler();
	}
	
	public MinecraftServer createGameServer(File folderLocation, File fileName)
	{
		return new MinecraftServer(folderLocation, fileName);
	}
	
	public void setup(Database db)
	{
		try
		{
			new MinecraftServerTable().createTable(db);
		}
		catch(SQLException e)
		{
			throw new RuntimeException(String.format("Error Creating tables: %s", e.getMessage()));
		}
	}
	
	public static void main(String[] args) {}
}
