package models;

import java.sql.Types;

import model.Column;
import model.TableTemp;

public class NodeTable extends TableTemp
{
	public static final Column<String> NAME =
			new Column<String>(String.class, "name", Types.VARCHAR, 100, true, false, false, null);
	
	public static final Column<Integer> MAX_RAM_ALLOWED =
			new Column<Integer>(Integer.class, "ram", Types.INTEGER, 0, false, false, false, null);
	
	public NodeTable()
	{
		super("node", NAME.typeClone(), MAX_RAM_ALLOWED.typeClone());
	}
}
