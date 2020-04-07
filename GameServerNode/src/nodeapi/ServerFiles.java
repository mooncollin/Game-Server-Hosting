package nodeapi;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nodemain.NodeProperties;
import utils.Utils;
import utils.servlet.Endpoint;
import utils.servlet.ParameterURL;

@WebServlet("/ServerFiles")
public class ServerFiles extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/ServerFiles";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
			Endpoint.Protocol.HTTP, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL getEndpoint(List<String> directories)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
		return url;
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var directory = ApiSettings.DIRECTORY.parse(request);
		
		if(!Utils.optionalsPresent(directory))
		{
			response.setStatus(400);
			return;
		}
		
		var currentDirectory = Paths.get(NodeProperties.getProperties().getProperty("deploy_folder"), directory.get().toArray(String[]::new)).toFile();
		
		if(!currentDirectory.isDirectory())
		{
			response.setStatus(400);
			return;
		}
		
		for(var file : currentDirectory.listFiles())
		{
			response.getWriter().println(String.format("%s,%s", file.getName(), file.isDirectory()));
		}
	}
}
