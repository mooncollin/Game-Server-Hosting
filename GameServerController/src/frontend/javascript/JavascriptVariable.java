package frontend.javascript;

import java.util.Objects;

public class JavascriptVariable <T>
{
	private String name;
	private T value;
	
	public JavascriptVariable(String name)
	{
		this(name, null);
	}
	
	public JavascriptVariable(String name, T value)
	{
		setName(name);
		setValue(value);
	}
	
	public void setName(String name)
	{
		this.name = Objects.requireNonNull(name);
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setValue(T value)
	{
		this.value = value;
	}
	
	public T getValue()
	{
		return value;
	}
	
	@Override
	public String toString()
	{
		var valueString = String.valueOf(value);
		if(value instanceof String)
		{
			valueString = String.format("\"%s\"", valueString.replace("\"", "\\\""));
		}
		
		return String.format("var %s = %s;", name, valueString);
	}
}
