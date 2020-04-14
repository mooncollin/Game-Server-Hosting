package minecraft.server;

import java.sql.SQLException;

import model.Database;
import model.Query;
import model.Table;
import models.GameServerTable;
import module.MinecraftGameServerModule;
import nodemain.StartUpApplication;
import server.GameServerOptions;

public class MinecraftServerOptions extends GameServerOptions<MinecraftServer>
{
	public static final String SERVER_TYPE = "minecraft";
	
	public MinecraftServerOptions()
	{
		this(null);
	}
	
	public MinecraftServerOptions(MinecraftServer server)
	{
		super(server);
	}
	
	@Override
	public boolean supportsAutoRestart()
	{
		return true;
	}
	
	@Override
	public boolean supportsArguments()
	{
		return true;
	}
	
	public Integer maxRam() throws SQLException
	{
		if(getServer() == null)
		{
			throw new IllegalStateException("This options instance does not hold a server");
		}
		
		var thisServer = Query.query(StartUpApplication.database, MinecraftServerTable.class)
							  .filter(MinecraftServerTable.ID, StartUpApplication.getID(getServer()))
							  .first();
		
		return thisServer.orElseThrow().getColumnValue(MinecraftServerTable.MAX_HEAP_SIZE);
	}
	
	@Override
	public String getServerType()
	{
		return SERVER_TYPE;
	}
	
	@Override
	public MinecraftServerCommandHandler getCommandHandler()
	{
		return new MinecraftServerCommandHandler(getServer());
	}
	
	@Override
	public void addGameServerTable(Table gameServerTable, Database db)
	{
		if(gameServerTable.getColumnValue(GameServerTable.ARGUMENTS) == null)
		{
			gameServerTable.setColumnValue(GameServerTable.ARGUMENTS, "");
		}
		
		var minecraftServer = new MinecraftServerTable();
		minecraftServer.setColumnValue(MinecraftServerTable.ID, gameServerTable.getColumnValue(GameServerTable.ID));
		minecraftServer.setColumnValue(MinecraftServerTable.MAX_HEAP_SIZE, MinecraftServer.MINIMUM_HEAP_SIZE);
		
		
		try
		{
			minecraftServer.commit(db);
			gameServerTable.commit(db);
		}
		catch(SQLException e)
		{
			MinecraftGameServerModule.LOGGER.error(String.format("Error creating minecraft server in database:\n%s", e.getMessage()));
		}
	}
}
