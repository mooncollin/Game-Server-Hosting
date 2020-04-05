package models;

import java.sql.Types;

import model.Column;
import model.Table;

public class NodeTable extends Table
{
	public static final Column<String> NAME =
			new Column<String>("name", Types.VARCHAR, 100, true, false, false, null);
	
	public static final Column<Integer> MAX_RAM_ALLOWED =
			new Column<Integer>("ram", Types.INTEGER, 0, false, false, false, null);
	
	public NodeTable()
	{
		super("node", NAME.typeClone(), MAX_RAM_ALLOWED.typeClone());
	}
}
