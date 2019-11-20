package models;

import java.sql.Types;

import model.Column;
import model.ColumnBuilder;
import model.ForeignKeyBuilder;
import model.Table;

public class MinecraftServerTable extends Table
{
	public static final Column<Integer> ID = ColumnBuilder.start(Integer.class, Types.INTEGER)
														  .setName("id")
														  .isPrimaryKey(true)
														  .isAutoIncremented(true)
														  .build();
	
	public static final Column<Integer> SERVER_ID = ColumnBuilder.start(Integer.class, Types.INTEGER)
															 	  .setName("serverid")
															 	  .setForeignKey(ForeignKeyBuilder.start(GameServerTable.ID)
															 			  						  .setTableReference(new GameServerTable())
															 			  		)
															 	  .build();
	
	public static final Column<Integer> MAX_HEAP_SIZE = ColumnBuilder.start(Integer.class, Types.INTEGER)
																	 .setName("maxheapsize")
																	 .build();
	
	public static final Column<Boolean> AUTO_RESTARTS = ColumnBuilder.start(Boolean.class, Types.BOOLEAN)
																	 .setName("restarts")
																	 .build();
	
	public static final Column<String> ARGUMENTS = ColumnBuilder.start(String.class, Column.TEXT)
																.setName("arguments")
																.build();
	
	public MinecraftServerTable()
	{
		super("minecraftserver", ID.typeClone(), SERVER_ID.typeClone(), MAX_HEAP_SIZE.typeClone(), AUTO_RESTARTS.typeClone(), ARGUMENTS.typeClone());
	}
}
