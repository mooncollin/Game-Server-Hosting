package utils.json;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONMessage
{
	Map<String, Object> data;
	
	public JSONMessage()
	{
		data = new HashMap<String, Object>();
	}
	
	public <T extends Number> void addInteger(String name, T number)
	{
		addObject(name, number);
	}
	
	public void addString(String name, String value)
	{
		addObject(name, value);
	}
	
	public void addBoolean(String name, Boolean value)
	{
		addObject(name, value);
	}
	
	public void addArray(String name, Object[] value)
	{
		addList(name, Arrays.asList(value));
	}
	
	public void addList(String name, List<?> value)
	{
		addObject(name, value);
	}
	
	public void addObject(String name, Object value)
	{
		data.put(name, value);
	}
	
	public Double getFloatingNumber(String name)
	{
		return getObject(name);
	}
	
	public Integer getIntegerNumber(String name)
	{
		return getObject(name);
	}
	
	public Boolean getBoolean(String name)
	{
		return getObject(name);
	}
	
	public String getString(String name)
	{
		return getObject(name);
	}
	
	public <T> List<T> getList(String name)
	{
		return getObject(name);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getObject(String name)
	{
		return (T) data.get(name);
	}
	
	public void parse(String value)
	{
		
	}
	
	@Override
	public String toString()
	{
		var result = "{\n";
		boolean second = false;
		
		for(var entry : data.entrySet())
		{
			var name = entry.getKey();
			var datum = entry.getValue();
			
			result += String.format("\"%s\": %s", name, getString(datum));
			
			if(second)
			{
				result += ",";
			}
			second = true;
			result += "\n";
		}
		
		return result += "\n}";
	}
	
	@SuppressWarnings("unchecked")
	private String getString(Object datum)
	{
		var valueString = "";
		if(datum == null || datum instanceof Integer || datum instanceof Boolean || datum instanceof Double)
		{
			valueString = String.valueOf(datum);
		}
		else if(datum instanceof String)
		{
			valueString = String.format("\"%s\"", datum);
		}
		else if(datum instanceof List)
		{
			valueString += "[\n";
			
			var listIt = ((List<Object>) datum).iterator();
			while(listIt.hasNext())
			{
				var item = listIt.next();
				valueString += getString(item);
				if(listIt.hasNext())
				{
					valueString += ",";
				}
				valueString += "\n";
			}
			
			valueString += "\n]";
		}
		else
		{
			valueString += "{\n";
			
//			for(var field : )
			
			valueString += "\n}";
		}
		
		return valueString;
	}
}
