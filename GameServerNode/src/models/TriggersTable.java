package models;

import java.sql.Types;

import model.Column;
import model.ColumnBuilder;
import model.ForeignKeyBuilder;
import model.Table;

public class TriggersTable extends Table
{
	public static final Column<Integer> ID = ColumnBuilder.start(Integer.class, Types.INTEGER)
														  .setName("id")
														  .isPrimaryKey(true)
														  .isAutoIncremented(true)
														  .build();
	
	public static final Column<String> TYPE = ColumnBuilder.start(String.class, Column.TEXT)
														   .setName("type")
														   .build();
	
	public static final Column<String> COMMAND = ColumnBuilder.start(String.class, Column.TEXT)
															  .setName("command")
															  .build();
	
	public static final Column<String> VALUE = ColumnBuilder.start(String.class, Column.TEXT)
															.setName("value")
															.build();
	
	public static final Column<Integer> SERVER_OWNER = ColumnBuilder.start(Integer.class, Types.INTEGER)
																	.setName("serverowner")
																	.setForeignKey(ForeignKeyBuilder.start(GameServerTable.ID)
																									.setTableReference(new GameServerTable()))
																	.build();
	
	public static final Column<String> EXTRA = ColumnBuilder.start(String.class, Column.TEXT)
															.setName("extra")
															.isNullable(true)
															.build();
	
	public TriggersTable()
	{
		super("triggers", ID.typeClone(), TYPE.typeClone(), COMMAND.typeClone(), VALUE.typeClone(), SERVER_OWNER.typeClone(), EXTRA.typeClone());
	}
}
