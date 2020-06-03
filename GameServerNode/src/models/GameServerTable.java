package models;

import java.sql.Types;

import model.Column;
import model.ColumnBuilder;
import model.ForeignKeyBuilder;
import model.Table;

/**
 * Table for holding information about a game server.
 * @author Collin
 *
 */
public class GameServerTable extends Table
{
	/**
	 * Unique identifier of the game server.
	 */
	public static final Column<Integer> ID = ColumnBuilder.<Integer>start(Types.INTEGER)
														  .setName("id")
														  .isPrimaryKey(true)
														  .isAutoIncremented(true)
														  .build();
	
	/**
	 * Name of the game server.
	 */
	public static final Column<String> NAME = ColumnBuilder.<String>start(Types.VARCHAR)
														   .setName("name")
														   .setLength(100)
														   .build();
	
	/**
	 * The name of the node that owns this game server.
	 */
	public static final Column<String> NODE_OWNER = ColumnBuilder.<String>start(Types.VARCHAR)
																 .setName("nodeOwner")
																 .setLength(45)
																 .setForeignKey(ForeignKeyBuilder.start(NodeTable.NAME)
																		 						 .setTableReference(new NodeTable()))
																 .build();
	
	/**
	 * The implementation type of this server.
	 */
	public static final Column<String> SERVER_TYPE = ColumnBuilder.<String>start(Types.VARCHAR)
																  .setName("servertype")
																  .setLength(45)
																  .build();
	
	/**
	 * Name of the executable to run when the server starts.
	 */
	public static final Column<String> EXECUTABLE_NAME = ColumnBuilder.<String>start(Types.VARCHAR)
																	  .setName("executableName")
																	  .setLength(100)
																	  .build();
	
	/**
	 * The auto restart feature of this game server. Null if not available.
	 */
	public static final Column<Boolean> AUTO_RESTARTS = ColumnBuilder.<Boolean>start(Types.BOOLEAN)
																	 .setName("autorestarts")
																	 .isNullable(true)
																	 .build();
	
	/**
	 * The command-line arguments to use when the game server starts.
	 */
	public static final Column<String> ARGUMENTS = ColumnBuilder.<String>start(Column.TEXT)
																.setName("arguments")
																.isNullable(true)
																.build();
	
	/**
	 * Constructor.
	 */
	public GameServerTable()
	{	
		super("gameserver", ID.typeClone(), NAME.typeClone(), NODE_OWNER.typeClone(), SERVER_TYPE.typeClone(), EXECUTABLE_NAME.typeClone(), AUTO_RESTARTS.typeClone(), ARGUMENTS.typeClone());
	}
}
