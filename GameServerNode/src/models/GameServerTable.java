package models;

import java.sql.Types;

import model.Column;
import model.ColumnBuilder;
import model.ForeignKeyBuilder;
import model.Table;

public class GameServerTable extends Table
{
	public static final Column<Integer> ID = ColumnBuilder.<Integer>start(Types.INTEGER)
														  .setName("id")
														  .isPrimaryKey(true)
														  .isAutoIncremented(true)
														  .build();
	
	public static final Column<String> NAME = ColumnBuilder.<String>start(Types.VARCHAR)
														   .setName("name")
														   .setLength(100)
														   .build();
	
	public static final Column<String> NODE_OWNER = ColumnBuilder.<String>start(Types.VARCHAR)
																 .setName("nodeOwner")
																 .setLength(45)
																 .setForeignKey(ForeignKeyBuilder.start(NodeTable.NAME)
																		 						 .setTableReference(new NodeTable()))
																 .build();
	
	public static final Column<String> SERVER_TYPE = ColumnBuilder.<String>start(Types.VARCHAR)
																  .setName("servertype")
																  .setLength(45)
																  .build();
	
	public static final Column<String> EXECUTABLE_NAME = ColumnBuilder.<String>start(Types.VARCHAR)
																	  .setName("executableName")
																	  .setLength(100)
																	  .build();
	
	public GameServerTable()
	{	
		super("gameserver", ID.typeClone(), NAME.typeClone(), NODE_OWNER.typeClone(), SERVER_TYPE.typeClone(), EXECUTABLE_NAME.typeClone());
	}
}
