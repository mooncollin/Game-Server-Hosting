package models;

import java.sql.Blob;
import java.sql.Types;

import model.Column;
import model.ColumnBuilder;
import model.Table;

public class GameModuleTable extends Table
{	
	public static final Column<String> NAME = ColumnBuilder.<String>start(Types.VARCHAR)
														   .setName("name")
														   .isPrimaryKey(true)
														   .setLength(100)
														   .build();
			
	public static final Column<Blob> JAR = ColumnBuilder.<Blob>start(Column.LONGBLOB)
														.setName("jar")
														.build();
														
	public static final Column<Blob> ICON = ColumnBuilder.<Blob>start(Column.LONGBLOB)
													     .setName("icon")
													     .build();
	
	public GameModuleTable()
	{
		super("gamemodule", NAME.typeClone(), JAR.typeClone(), ICON.typeClone());
	}
}
