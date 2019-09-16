package utils;

public class StringUtils
{
	public static String capitalize(String in)
	{
		return in.substring(0, 1).toUpperCase() + in.substring(1);
	}
}
