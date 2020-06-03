package utils.servlet;

import utils.servlet.Endpoint.Protocol;

public class EndpointBuilder
{
	private Protocol protocol;
	private String host;
	private Integer port;
	private String name;
	private String endpoint;
	
	private EndpointBuilder(String name)
	{
		this.name = name;
	}
	
	public static EndpointBuilder start(String name)
	{
		return new EndpointBuilder(name);
	}
	
	public EndpointBuilder setProtocol(Protocol protocol)
	{
		this.protocol = protocol;
		return this;
	}
	
	public EndpointBuilder setHost(String host)
	{
		this.host = host;
		return this;
	}
	
	public EndpointBuilder setPort(Integer port)
	{
		this.port = port;
		return this;
	}
	
	public EndpointBuilder setEndpoint(String endpoint)
	{
		this.endpoint = endpoint;
		return this;
	}
	
	public Endpoint build()
	{
		return new Endpoint(name, protocol, host, port, endpoint);
	}
}
