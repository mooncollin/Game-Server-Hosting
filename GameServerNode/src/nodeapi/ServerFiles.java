package nodeapi;

import java.io.IOException;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nodemain.NodeProperties;
import utils.Utils;

@WebServlet(
		name = "ServerFiles",
		urlPatterns = "/ServerFiles",
		asyncSupported = true
)
public class ServerFiles extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
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
