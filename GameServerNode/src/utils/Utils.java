package utils;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
	
	public static String encodeURL(String url)
	{
		url = new String(url.getBytes(), StandardCharsets.UTF_8);
		var queryIndex = url.indexOf("?");
		queryIndex = queryIndex == -1 ? url.length() : queryIndex;
		var baseURL = url.substring(0, queryIndex);
		var queryURL = URLEncoder.encode(url.substring(queryIndex), StandardCharsets.UTF_8);
		
		return (baseURL + queryURL).replaceAll("%20", "+")
				.replaceAll("%3A", ":")
                .replaceAll("%2F", "/")
                .replaceAll("%3B", ";")
                .replaceAll("%40", "@")
                .replaceAll("%3C", "<")
                .replaceAll("%3E", ">")
                .replaceAll("%3D", "=")
                .replaceAll("%26", "&")
                .replaceAll("%25", "%")
                .replaceAll("%24", "$")
                .replaceAll("%23", "#")
                .replaceAll("%2B", "+")
                .replaceAll("%2C", ",")
                .replaceAll("%3F", "?")
                .replaceAll(" ", "+");
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
