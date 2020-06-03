package models;

import java.sql.Blob;
import java.sql.Types;

import model.Column;
import model.ColumnBuilder;
import model.Table;

/**
 * The table holding the information of a module.
 * @author Collin
 *
 */
public class GameModuleTable extends Table
{
	/**
	 * Name of the module.
	 */
	public static final Column<String> NAME = ColumnBuilder.<String>start(Types.VARCHAR)
														   .setName("name")
														   .isPrimaryKey(true)
														   .setLength(100)
														   .build();
	
	/**
	 * Jar binary implementation of the module.
	 */
	public static final Column<Blob> JAR = ColumnBuilder.<Blob>start(Column.LONGBLOB)
														.setName("jar")
														.build();
	
	/**
	 * Icon binary of the module. Just an image in base64.
	 */
	public static final Column<Blob> ICON = ColumnBuilder.<Blob>start(Column.LONGBLOB)
													     .setName("icon")
													     .build();
	
	/**
	 * Constructor.
	 */
	public GameModuleTable()
	{
		super("gamemodule", NAME.typeClone(), JAR.typeClone(), ICON.typeClone());
	}
}
