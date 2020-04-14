package utils;

import java.util.Objects;

public class StringUtils
{
	/**
	 * Capitalizes the first character.
	 * @param in input string
	 * @return a string where the first character has been capitalized
	 */
	public static String capitalize(String in)
	{
		Objects.requireNonNull(in);
		if(in.length() < 1)
		{
			return in;
		}
		
		return Character.toUpperCase(in.charAt(0)) + in.substring(1);
	}
	
	/**
	 * Capitalizes every word deliminated by spaces.
	 * @param in input string
	 * @return a string where every word is capitalized
	 */
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
