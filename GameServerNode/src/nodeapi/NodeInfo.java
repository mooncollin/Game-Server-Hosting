package nodeapi;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nodemain.NodeProperties;
import utils.Utils;
import utils.servlet.Endpoint;
import utils.servlet.HttpStatus;
import utils.servlet.ParameterURL;

@WebServlet(
	name = "NodeInfo",
	urlPatterns = "/NodeInfo",
	asyncSupported = true
)
public class NodeInfo extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/NodeInfo";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
			Endpoint.Protocol.HTTP, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL getEndpoint(String property)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.PROPERTY.getName(), property);
		return url;
	}
	
	public static final Map<String, String> PROPERTY_COMMANDS = Map.ofEntries
	(
			Map.entry("deploy_folder", NodeProperties.getProperties().getProperty("deploy_folder"))
	);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var property = ApiSettings.PROPERTY.parse(request);
		
		if(!Utils.optionalsPresent(property))
		{
			response.setStatus(HttpStatus.BAD_REQUEST.getCode());
			return;
		}
		
		var responseValue = PROPERTY_COMMANDS.get(property.get());
		if(responseValue == null)
		{
			response.setStatus(HttpStatus.BAD_REQUEST.getCode());
			return;
		}
		
		response.getWriter().print(responseValue);
	}
}
