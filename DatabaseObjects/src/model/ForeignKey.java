package model;

public class ForeignKey <T extends Comparable<T>>
{
	private final String name;
	private final Table tableReference;
	private final Column<T> columnReference;
	
	public ForeignKey(String name, Table table, Column<T> column)
	{
		this.name = name;
		this.tableReference = table;
		this.columnReference = column;
	}
	
	public String getName()
	{
		return name;
	}
	
	public Table getTableReference()
	{
		return tableReference;
	}
	
	public Column<T> getColumnReference()
	{
		return columnReference;
	}
}
