package models;

import java.util.Map;

import model.Column;
import model.Model;
import model.Table;

@Table("minecraftserver")
public class MinecraftServer extends Model
{
	@Column(columnName="id", methodName="setID", methodParameter=Integer.class, autoGenerated=true, primaryKey=true)
	private Integer id;
	
	@Column(columnName="maxheapsize", methodName="setMaxHeapSize", methodParameter=Integer.class)
	private Integer maxheapsize;
	
	@Column(columnName="restarts", methodName="setRestarts", methodParameter=Boolean.class)
	private Boolean restarts;
	
	@Column(columnName="arguments", methodName="setArguments", methodParameter=String.class)
	private String arguments;
	
	public MinecraftServer(Integer maxheapsize, Boolean restarts, String arguments)
	{
		setMaxHeapSize(maxheapsize);
		setRestarts(restarts);
		setArguments(arguments);
	}
	
	public MinecraftServer(Map<String, Object> columnParameters)
	{
		super(columnParameters);
	}
	
	public void setID(Integer id)
	{
		if(checkVariable(this.id, id))
		{
			changed = true;
			this.id = id;
		}
	}
	
	public void setMaxHeapSize(Integer maxheapsize)
	{
		if(checkVariable(this.maxheapsize, maxheapsize))
		{
			changed = true;
			this.maxheapsize = maxheapsize;
		}
	}
	
	public void setRestarts(Boolean restarts)
	{
		if(checkVariable(this.restarts, restarts))
		{
			changed = true;
			this.restarts = restarts;
		}
	}
	
	public void setArguments(String arguments)
	{
		if(checkVariable(this.arguments, arguments))
		{
			changed = true;
			this.arguments = arguments;
		}
	}
	
	public Boolean getRestarts()
	{
		return restarts;
	}
	
	public Integer getID()
	{
		return id;
	}
	
	public Integer getMaxHeapSize()
	{
		return maxheapsize;
	}
	
	public String getArguments()
	{
		return arguments;
	}
	
	protected void resetAutoGenerated()
	{
		setID(-1);
	}
}
