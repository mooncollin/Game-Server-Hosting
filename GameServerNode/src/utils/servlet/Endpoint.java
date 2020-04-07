package utils.servlet;

import javax.servlet.http.HttpServlet;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.annotation.WebServlet;

abstract public class Endpoint
{
	public static enum HttpMethod
	{
		GET("GET"), POST("POST");
		
		private String value;
		
		private HttpMethod(String value)
		{
			this.value = value;
		}
		
		public String getValue()
		{
			return value;
		}
		
		@Override
		public String toString()
		{
			return value;
		}
	}
	
	public static enum Protocol
	{
		HTTP("http"), HTTPS("https"), WEB_SOCKET("ws");
		
		private String value;
		
		private Protocol(String value)
		{
			this.value = value;
		}
		
		public String getValue()
		{
			return value;
		}
		
		@Override
		public String toString()
		{
			return value;
		}
	}
	
	public static <T extends HttpServlet> String getServletName(Class<T> servlet)
	{
		var annotation = servlet.getDeclaredAnnotation(WebServlet.class);
		if(annotation == null)
		{
			return null;
		}
		
		return annotation.name();
	}
	
	public static <T extends HttpServlet> String[] getServletUrlPatterns(Class<T> servlet)
	{
		var annotation = servlet.getDeclaredAnnotation(WebServlet.class);
		if(annotation == null)
		{
			return null;
		}
		
		return annotation.urlPatterns();
	}
	
	@SuppressWarnings("unchecked")
	public static String[] getServletUrlPatterns(String name)
	{
		try
		{
			return getServletUrlPatterns((Class<? extends HttpServlet>) Class.forName(name));
		} catch (Exception e)
		{
			return null;
		}
	}
	
	public static <T extends HttpServlet> String getServletURL(Class<T> servlet)
	{
		return getServletUrlPatterns(servlet)[0];
	}
	
	public static String getServletURL(String name)
	{
		return getServletUrlPatterns(name)[0];
	}
	
	public static String getQueryString(Map<String, ?> entries)
	{
		return String.format("?%s", 
			String.join("&", entries.entrySet()
									.stream()
									.map(entry -> String.format("%s=%s", entry.getKey(), URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8)))
									.toArray(String[]::new))
		);
	}
	
	private final Protocol protocol;
	private final String host;
	private final Integer port;
	private final String name;
	private final String endpoint;
	
	public <T extends HttpServlet> Endpoint(Class<T> servlet, String host, Integer port)
	{
		this(getServletName(servlet), null, host, port, getServletURL(servlet));
	}
	
	public <T extends HttpServlet> Endpoint(Class<T> servlet, String prefix, String host, Integer port)
	{
		this(getServletName(servlet), null, host, port, prefix + getServletURL(servlet));
	}
	
	public <T extends HttpServlet> Endpoint(Class<T> servlet, String host)
	{
		this(getServletName(servlet), null, host, null, getServletURL(servlet));
	}
	
	public <T extends HttpServlet> Endpoint(Class<T> servlet)
	{
		this(getServletName(servlet), null, null, null, getServletURL(servlet));
	}
	
	public Endpoint(String name, Protocol protocol, String host, Integer port, String endpoint)
	{
		this.name = name;
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.endpoint = endpoint;
	}
	
	public String getName()
	{
		return name;
	}
	
	public Protocol getProtocol()
	{
		return protocol;
	}
	
	public String getHost()
	{
		return host;
	}
	
	public Integer getPort()
	{
		return port;
	}
	
	public String getEndpoint()
	{
		return endpoint;
	}
	
	public ParameterURL getRequestURL()
	{
		return new ParameterURL(protocol, host, port, endpoint);
	}
	
	@Override
	public String toString()
	{
		return getRequestURL().getURL();
	}
	
	abstract public ParameterURL get(Object... values);
	abstract public ParameterURL post(Object... values);
}
