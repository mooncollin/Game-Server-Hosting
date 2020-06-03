package models;

import java.sql.Types;

import model.Column;
import model.ColumnBuilder;
import model.Table;

/**
 * Table for holding information about a node.
 * @author Collin
 *
 */
public class NodeTable extends Table
{
	/**
	 * Name of the node.
	 */
	public static final Column<String> NAME = ColumnBuilder.<String>start(Types.VARCHAR)
														   .setName("name")
														   .setLength(100)
														   .isPrimaryKey(true)
														   .build();
	
	/**
	 * Constructor.
	 */
	public NodeTable()
	{
		super("node", NAME.typeClone());
	}
}
