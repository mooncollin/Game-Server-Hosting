package frontend.javascript;

import java.util.Objects;

public class JavascriptVariable <T>
{
	public static String escapeString(String input)
	{
		input = input.replace("\"", "\\\"");
		input = input.replace("\\", "\\\\");
		return String.format("\"%s\"", input);
	}
	
	private String name;
	private T value;
	private boolean isConst;
	
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
	
	public boolean isConst()
	{
		return isConst;
	}
	
	public void setConst(boolean c)
	{
		this.isConst = c;
	}
	
	public String valueString()
	{
		var valueString = String.valueOf(value);
		if(value instanceof String)
		{
			valueString = escapeString(valueString);
		}
		else if(value instanceof JavascriptVariable)
		{
			valueString = ((JavascriptVariable<?>) value).valueString();
		}
		
		return valueString;
	}
	
	public String variableString()
	{
		return String.format("%svar %s", isConst ? "const " : "", name);
	}
	
	@Override
	public String toString()
	{
		return String.format("%s = %s;", variableString(), valueString());
	}
}
