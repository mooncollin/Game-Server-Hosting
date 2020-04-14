package server;

import java.sql.SQLException;

import model.Database;
import model.Query;
import model.Table;
import models.GameServerTable;
import nodemain.StartUpApplication;

abstract public class GameServerOptions<T extends GameServer>
{
	private final T server;
	
	public GameServerOptions()
	{
		this(null);
	}
	
	public GameServerOptions(T server)
	{
		this.server = server;
	}
	
	public T getServer()
	{
		return server;
	}
	
	public boolean supportsAutoRestart()
	{
		return false;
	}
	
	public boolean supportsArguments()
	{
		return false;
	}
	
	public Boolean autoRestarts() throws SQLException
	{
		if(getServer() == null)
		{
			throw new IllegalStateException("This options instance does not hold a server");
		}
		
		var thisServer = Query.query(StartUpApplication.database, GameServerTable.class)
							  .filter(GameServerTable.ID, StartUpApplication.getID(getServer()))
							  .first();
		
		return thisServer.orElseThrow().getColumnValue(GameServerTable.AUTO_RESTARTS);
	}
	
	public String arguments() throws SQLException
	{
		if(getServer() == null)
		{
			throw new IllegalStateException("This options instance does not hold a server");
		}
		
		var thisServer = Query.query(StartUpApplication.database, GameServerTable.class)
							  .filter(GameServerTable.ID, StartUpApplication.getID(getServer()))
							  .first();
		
		return thisServer.orElseThrow().getColumnValue(GameServerTable.ARGUMENTS);
	}
	
	public String executableName() throws SQLException
	{
		if(getServer() == null)
		{
			throw new IllegalStateException("This options instance does not hold a server");
		}
		
		var thisServer = Query.query(StartUpApplication.database, GameServerTable.class)
							  .filter(GameServerTable.ID, StartUpApplication.getID(getServer()))
							  .first();
		
		return thisServer.orElseThrow().getColumnValue(GameServerTable.EXECUTABLE_NAME);
	}
	
	abstract public String getServerType();
	
	public void addGameServerTable(Table gameServerTable, Database db)
	{
		
	}
	
	public GameServerCommandHandler<T> getCommandHandler()
	{
		return new GameServerCommandHandler<T>(server);
	}
}
