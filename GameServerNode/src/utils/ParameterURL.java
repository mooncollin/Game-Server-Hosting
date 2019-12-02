package utils;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ParameterURL
{
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	public static final String HTTP_PROTOCOL = "http";
	public static final String WEB_SOCKET_PROTOCOL = "ws";
	
	private String protocol;
	private String host;
	private Integer port;
	private String endpoint;
	private Charset encoding;
	
	private Map<String, String> queryParameters;
	
	public ParameterURL(String protocol, String host, Integer port, String endpoint)
	{
		this.queryParameters = new HashMap<String, String>();
		setProtocol(protocol);
		setHost(host);
		setPort(port);
		setEndpoint(endpoint);
		setEncoding(DEFAULT_CHARSET);
	}
	
	public ParameterURL(final ParameterURL other)
	{
		this.queryParameters = new HashMap<String, String>(other.queryParameters);
		setProtocol(other.protocol);
		setHost(other.host);
		setPort(other.port);
		setEndpoint(other.endpoint);
		setEncoding(other.encoding);
	}
	
	public void setEncoding(Charset encoding)
	{
		this.encoding = Objects.requireNonNull(encoding);
	}
	
	public Charset getEncoding()
	{
		return encoding;
	}
	
	public void setProtocol(String protocol)
	{
		this.protocol = Objects.requireNonNullElse(protocol, "");
	}
	
	public String getProtocol()
	{
		return protocol;
	}
	
	public void setHost(String host)
	{
		this.host = Objects.requireNonNullElse(host, "");
	}
	
	public String getHost()
	{
		return host;
	}
	
	public void setPort(Integer port)
	{
		this.port = port;
	}
	
	public int getPort()
	{
		return port;
	}
	
	public void setEndpoint(String endpoint)
	{
		this.endpoint = Objects.requireNonNullElse(endpoint, "");
		if(this.endpoint.startsWith("/"))
		{
			this.endpoint = this.endpoint.replaceFirst("/", "");
		}
	}
	
	public String getEndpoint()
	{
		return endpoint;
	}
	
	public void addQuery(String key, Object value)
	{
		addQuery(key, String.valueOf(value));
	}
	
	public void addQuery(String key, String value)
	{
		queryParameters.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
	}
	
	public void addQueries(Map<String, String> queries)
	{
		queryParameters.putAll(Objects.requireNonNull(queries));
	}
	
	public String removeQuery(String key)
	{
		return queryParameters.remove(key);
	}
	
	public void clearQueries()
	{
		queryParameters.clear();
	}
	
	@SuppressWarnings("unchecked")
	public void addQueries(Map.Entry<String, String>... entries)
	{
		for(var entry : Objects.requireNonNull(entries))
		{
			addQuery(entry.getKey(), entry.getValue());
		}
	}
	
	public String getURL()
	{
		return toString();
	}
	
	@Override
	public String toString()
	{
		return String.format("%s%s%s%s/%s%s%s", 
			protocol, 
			protocol.isEmpty() ? "" : "://",
			host,
			port == null ? "" :  ":" + String.valueOf(port),
			endpoint,
			queryParameters.isEmpty() ? "" : "?",
			String.join("&", queryParameters.entrySet()
					.stream()
					.map(entry -> String.format("%s=%s", entry.getKey(), URLEncoder.encode(entry.getValue(), encoding)))
					.toArray(String[]::new)
			)
		);
	}
}
