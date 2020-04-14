package models;

import java.sql.SQLException;
import java.util.NoSuchElementException;

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
}
