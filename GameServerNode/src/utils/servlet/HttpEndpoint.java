package utils.servlet;

import java.util.Objects;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

abstract public class HttpEndpoint extends Endpoint
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
	
	public <T extends HttpServlet> HttpEndpoint(Class<T> servlet, String host, Integer port)
	{
		super(getServletName(servlet), Endpoint.Protocol.HTTP, host, port, getServletURL(servlet));
	}
	
	public <T extends HttpServlet> HttpEndpoint(Class<T> servlet, String host)
	{
		this(servlet, host, null);
	}
	
	public <T extends HttpServlet> HttpEndpoint(Class<T> servlet)
	{
		this(servlet, null, null);
	}
	
	public <T extends HttpServlet> HttpEndpoint(Class<T> servlet, Integer port)
	{
		this(servlet, null, port);
	}
	
	abstract public ParameterURL get(Object... values);
	abstract public ParameterURL post(Object... values);
	
	@Override
	public HttpEndpoint relative()
	{
		super.relative();
		return this;
	}
	
	@Override
	public HttpEndpoint relative(String prefix)
	{
		super.relative(prefix);
		return this;
	}
	
	public HttpEndpoint absolute(String host)
	{
		setProtocol(Endpoint.Protocol.HTTP);
		setHost(host);
		return this;
	}
	
	public HttpEndpoint prefix(String prefix)
	{
		setEndpoint(prefix + Objects.requireNonNullElse(getEndpoint(), ""));
		return this;
	}
}
