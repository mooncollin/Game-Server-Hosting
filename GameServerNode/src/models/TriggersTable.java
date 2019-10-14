package models;

import java.sql.Types;

import model.Column;
import model.Table;

public class TriggersTable extends Table
{
	public static final Column<Integer> ID =
			new Column<Integer>(Integer.class, "id", Types.INTEGER, 0, true, false, true, null);
	
	public static final Column<String> TYPE =
			new Column<String>(String.class, "type", Column.TEXT, 0, false, false, false, null);
	
	public static final Column<String> COMMAND =
			new Column<String>(String.class, "command", Column.TEXT, 0, false, false, false, null);
	
	public static final Column<String> VALUE =
			new Column<String>(String.class, "value", Column.TEXT, 0, false, false, false, null);
	
	public static final Column<String> SERVER_OWNER =
			new Column<String>(String.class, "serverowner", Column.TEXT, 0, false, false, false, null);
	
	public static final Column<String> EXTRA =
			new Column<String>(String.class, "extra", Column.TEXT, 0, false, true, false, null);
	
	public TriggersTable()
	{
		super("triggers", ID.typeClone(), TYPE.typeClone(), COMMAND.typeClone(), VALUE.typeClone(), SERVER_OWNER.typeClone(), EXTRA.typeClone());
	}
}
