package utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Utils
{
	public static int map(int in, int in_min, int in_max, int out_min, int out_max)
	{
		int denom = in_max - in_min;
		if(denom == 0)
		{
			return 0;
		}
		
		return (in - in_min) * (out_max - out_min) / denom + out_min;
	}
	
	public static String encodeURL(String url)
	{
		url = new String(url.getBytes(), StandardCharsets.UTF_8);
		int queryIndex = url.indexOf("?");
		queryIndex = queryIndex == -1 ? url.length() : queryIndex;
		String baseURL = url.substring(0, queryIndex);
		String queryURL = URLEncoder.encode(url.substring(queryIndex), StandardCharsets.UTF_8);
		
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
}
