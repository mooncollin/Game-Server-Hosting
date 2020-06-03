package models;

import java.sql.Types;

import model.Column;
import model.ColumnBuilder;
import model.ForeignKeyBuilder;
import model.Table;

/**
 * Table for holding trigger information.
 * @author Collin
 *
 */
public class TriggersTable extends Table
{
	/**
	 * Unique identifier for the trigger.
	 */
	public static final Column<Integer> ID = ColumnBuilder.<Integer>start(Types.INTEGER)
														  .setName("id")
														  .isPrimaryKey(true)
														  .isAutoIncremented(true)
														  .build();
	
	/**
	 * Type of the trigger.
	 */
	public static final Column<String> TYPE = ColumnBuilder.<String>start(Column.TEXT)
														   .setName("type")
														   .build();
	
	/**
	 * The command to run when this trigger activates.
	 */
	public static final Column<String> COMMAND = ColumnBuilder.<String>start(Column.TEXT)
															  .setName("command")
															  .build();
	
	/**
	 * Information related to how the trigger gets activated. This information is interpreted
	 * differently depending on the type of the trigger.
	 */
	public static final Column<String> VALUE = ColumnBuilder.<String>start(Column.TEXT)
															.setName("value")
															.build();
	
	/**
	 * The unique identifier of the game server that this trigger is related to.
	 */
	public static final Column<Integer> SERVER_OWNER = ColumnBuilder.<Integer>start(Types.INTEGER)
																	.setName("serverowner")
																	.setForeignKey(ForeignKeyBuilder.start(GameServerTable.ID)
																									.setTableReference(new GameServerTable()))
																	.build();
	
	/**
	 * Action information such as starting a server or stopping a server.
	 */
	public static final Column<String> EXTRA = ColumnBuilder.<String>start(Column.TEXT)
															.setName("extra")
															.isNullable(true)
															.build();
	
	/**
	 * Constructor.
	 */
	public TriggersTable()
	{
		super("triggers", ID.typeClone(), TYPE.typeClone(), COMMAND.typeClone(), VALUE.typeClone(), SERVER_OWNER.typeClone(), EXTRA.typeClone());
	}
}
