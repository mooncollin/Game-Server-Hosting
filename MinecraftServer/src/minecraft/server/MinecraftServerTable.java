package minecraft.server;

import java.sql.Types;

import model.Column;
import model.ColumnBuilder;
import model.ForeignKeyBuilder;
import model.Table;
import models.GameServerTable;

public class MinecraftServerTable extends Table
{	
	public static final Column<Integer> ID = ColumnBuilder.<Integer>start(Types.INTEGER)
															 	  .setName("id")
															 	  .isPrimaryKey(true)
															 	  .setForeignKey(ForeignKeyBuilder.start(GameServerTable.ID)
															 			  						  .setTableReference(new GameServerTable())
															 			  		)
															 	  .build();
	
	public static final Column<Integer> MAX_HEAP_SIZE = ColumnBuilder.<Integer>start(Types.INTEGER)
																	 .setName("maxheapsize")
																	 .build();
	
	public MinecraftServerTable()
	{
		super("minecraftserver", ID.typeClone(), MAX_HEAP_SIZE.typeClone());
	}
}
