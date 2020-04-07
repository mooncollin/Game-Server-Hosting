package models;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import model.Query;
import nodemain.StartUpApplication;

public class Utils
{
	public static String getServerType(int serverID) throws SQLException, NoSuchElementException
	{
		var server = Query.query(StartUpApplication.database, GameServerTable.class)
						  .filter(GameServerTable.ID, serverID)
						  .first();
		
		return server.orElseThrow().getColumnValue(GameServerTable.SERVER_TYPE);
	}
	
	public static Set<String> getServerTypes() throws SQLException
	{
		var servers = Query.query(StartUpApplication.database, GameServerTable.class)
					  	   .all();
		
		var serverTypes = servers.stream()
								 .map(s -> s.getColumnValue(GameServerTable.SERVER_TYPE))
								 .collect(Collectors.toSet());
		
		return serverTypes;
	}
}
