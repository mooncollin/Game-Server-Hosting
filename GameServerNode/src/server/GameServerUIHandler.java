package server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;

abstract public class GameServerUIHandler
{
	private Map<String, HttpServlet> servlets;
	
	public GameServerUIHandler()
	{
		servlets = new HashMap<String, HttpServlet>();
	}
	
	protected void registerServlet(String servletName, HttpServlet servlet)
	{
		servlets.put(servletName, servlet);
	}
	
	protected void removeServlet(String servletName)
	{
		servlets.remove(servletName);
	}
	
	public final Map<String, HttpServlet> getRegisteredServlets()
	{
		return Collections.unmodifiableMap(servlets);
	}
	
	abstract public String getIconClass(String servletName);
}
