package models;

import java.sql.Types;

import model.Column;
import model.TableTemp;

public class MinecraftServerTable extends TableTemp
{
	public static final Column<Integer> ID =
			new Column<Integer>(Integer.class, "id", Types.INTEGER, 0, true, false, true, null);
	
	public static final Column<Integer> MAX_HEAP_SIZE =
			new Column<Integer>(Integer.class, "maxheapsize", Types.INTEGER, 0, false, false, false, null);
	
	public static final Column<Boolean> AUTO_RESTARTS = 
			new Column<Boolean>(Boolean.class, "restarts", Types.BOOLEAN, 0, false, false, false, null);
	
	public static final Column<String> ARGUMENTS =
			new Column<String>(String.class, "arguments", Column.TEXT, 0, false, false, false, null);
	
	public MinecraftServerTable()
	{
		super("minecraftserver", ID.typeClone(), MAX_HEAP_SIZE.typeClone(), AUTO_RESTARTS.typeClone(), ARGUMENTS.typeClone());
	}
}
