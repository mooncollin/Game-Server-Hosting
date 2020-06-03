package utils.servlet;

import java.util.Objects;

public class Endpoint
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
	
	private Protocol protocol;
	private String host;
	private Integer port;
	private String name;
	private String endpoint;
	
	public Endpoint(String name, Protocol protocol, String host, Integer port, String endpoint)
	{
		this.name = name;
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.endpoint = endpoint;
	}
	
	public Endpoint local()
	{
		this.protocol = null;
		return this;
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
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public void setProtocol(Protocol p)
	{
		this.protocol = p;
	}
	
	public void setPort(Integer port)
	{
		this.port = port;
	}
	
	public void setEndpoint(String endpoint)
	{
		this.endpoint = endpoint;
	}
	
	public void setHost(String host)
	{
		this.host = host;
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
	
	public Endpoint relative()
	{
		setProtocol(null);
		setHost(null);
		setPort(null);
		return this;
	}
	
	public Endpoint relative(String prefix)
	{
		setProtocol(null);
		setHost(null);
		setPort(null);
		prefix(prefix);
		return this;
	}
	
	public Endpoint prefix(String prefix)
	{
		setEndpoint(prefix + Objects.requireNonNullElse(getEndpoint(), ""));
		return this;
	}
}
