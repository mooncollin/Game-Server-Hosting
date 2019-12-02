package utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Utils
{
	private static Map<Class<?>, Method> CONVERTERS = new HashMap<Class<?>, Method>();
	
	public static int map(int in, int in_min, int in_max, int out_min, int out_max)
	{
		var denom = in_max - in_min;
		if(denom == 0)
		{
			return 0;
		}
		
		return (in - in_min) * (out_max - out_min) / denom + out_min;
	}
	
	public static <T> T fromString(Class<T> clazz, String str)
	{
		return fromString(clazz, str, null);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T fromString(Class<T> clazz, String str, T defaultValue)
	{
		var method = CONVERTERS.get(clazz);
		if(method == null)
		{
			try
			{
				method = clazz.getDeclaredMethod("valueOf", String.class);
				CONVERTERS.put(clazz, method);
			} catch (NoSuchMethodException e)
			{
				return defaultValue;
			}
		}
		
		try
		{
			return (T) method.invoke(null, str);
		}
		catch(Exception e)
		{
			return defaultValue;
		}
	}
}
