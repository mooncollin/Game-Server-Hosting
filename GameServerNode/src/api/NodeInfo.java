package api;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.NodeProperties;

@WebServlet("/NodeInfo")
public class NodeInfo extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/NodeInfo";
	
	public static String getEndpoint(String property)
	{
		return String.format("%s?property=%s", URL, property);
	}
	
	public static final Map<String, String> PROPERTY_COMMANDS = Map.ofEntries
	(
			Map.entry("deploy_folder", NodeProperties.getProperties().getProperty("deploy_folder"))
	);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var property = request.getParameter("property");
		
		if(property == null)
		{
			response.setStatus(400);
			return;
		}
		
		var responseValue = PROPERTY_COMMANDS.get(property);
		if(responseValue == null)
		{
			response.setStatus(400);
			return;
		}
		
		response.getWriter().print(responseValue);
	}
}
