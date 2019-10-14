package model;

public class ForeignKey <T extends Comparable<T>>
{
	private final String name;
	private final TableTemp tableReference;
	private final Column<T> columnReference;
	
	public ForeignKey(String name, TableTemp table, Column<T> column)
	{
		this.name = name;
		this.tableReference = table;
		this.columnReference = column;
	}
	
	public String getName()
	{
		return name;
	}
	
	public TableTemp getTableReference()
	{
		return tableReference;
	}
	
	public Column<T> getColumnReference()
	{
		return columnReference;
	}
}
