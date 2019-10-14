package models;

import java.sql.Types;

import model.Column;
import model.TableTemp;

public class GameServerTable extends TableTemp
{
	public static final Column<String> NAME =
			new Column<String>(String.class, "name", Types.VARCHAR, 100, true, false, false, null);
	
	public static final Column<String> NODE_OWNER =
			new Column<String>(String.class, "nodeOwner", Types.VARCHAR, 45, false, false, false, null);
	
	public static final Column<Integer> SPECIFIC_ID =
			new Column<Integer>(Integer.class, "specificid", Types.INTEGER, 0, false, true, false, null);
	
	public static final Column<String> SERVER_TYPE =
			new Column<String>(String.class, "servertype", Types.VARCHAR, 45, false, false, false, null);
	
	public static final Column<String> EXECUTABLE_NAME =
			new Column<String>(String.class, "executableName", Types.VARCHAR, 100, false, false, false, null);
	
	public GameServerTable()
	{
		super("gameserver", NAME.typeClone(), NODE_OWNER.typeClone(), SPECIFIC_ID.typeClone(),
			SERVER_TYPE.typeClone(), EXECUTABLE_NAME.typeClone());
	}
}
