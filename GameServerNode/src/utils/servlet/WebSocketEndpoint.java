package utils.servlet;

import java.util.Objects;

import javax.websocket.server.ServerEndpoint;

abstract public class WebSocketEndpoint extends Endpoint
{
	public static String getWebEndpointPattern(Class<?> endpointClass)
	{
		var annotation = endpointClass.getDeclaredAnnotation(ServerEndpoint.class);
		if(annotation == null)
		{
			return null;
		}
		
		return annotation.value();
	}
	
	public WebSocketEndpoint(Class<?> endpointClass, String name)
	{
		this(endpointClass, name, null, null);
	}
	
	public WebSocketEndpoint(Class<?> endpointClass, String name, Integer port)
	{
		this(endpointClass, name, null, port);
	}
	
	public WebSocketEndpoint(Class<?> endpointClass, String name, String host)
	{
		this(endpointClass, name, host, null);
	}
	
	public WebSocketEndpoint(Class<?> endpointClass, String name, String host, Integer port)
	{
		super(name, Endpoint.Protocol.WEB_SOCKET, host, port, getWebEndpointPattern(endpointClass));
	}
	
	abstract public ParameterURL open(Object... values);
	
	public WebSocketEndpoint prefix(String prefix)
	{
		setEndpoint(prefix + Objects.requireNonNullElse(getEndpoint(), ""));
		return this;
	}
}
