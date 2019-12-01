package utils;

import java.util.Objects;

public class StringUtils
{
	public static String capitalize(String in)
	{
		Objects.requireNonNull(in);
		if(in.length() < 1)
		{
			return in;
		}
		
		return Character.toUpperCase(in.charAt(0)) + in.substring(1);
	}
	
	public static String titleize(String in)
	{
		var words = in.split(" ");
		for(var i = 0; i < words.length; i++)
		{
			words[i] = capitalize(words[i]);
		}
		
		return String.join(" ", words);
	}
}
