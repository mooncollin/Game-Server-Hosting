package nodeapi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nodemain.NodeProperties;
import utils.ParameterURL;

@WebServlet("/FileDelete")
public class FileDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/FileDelete";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		ParameterURL.HTTP_PROTOCOL, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL getEndpoint(String directory)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.DIRECTORY_PARAMETER, directory);
		return url;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var directory = request.getParameter(ApiSettings.DIRECTORY_PARAMETER);
		if(directory == null)
		{
			response.setStatus(400);
			return;
		}
		
		var directories = directory.split(",");
		
		var currentFile = Paths.get(NodeProperties.DEPLOY_FOLDER, directories).toFile();
		if(!currentFile.exists() || currentFile.equals(Paths.get(NodeProperties.DEPLOY_FOLDER).toFile()))
		{
			response.setStatus(400);
			return;
		}
		
		deleteDirectoryOrFile(currentFile);
	}
	
	public static void deleteDirectoryOrFile(File directory)
	{
		if(directory.isDirectory())
		{
			for(var f : directory.listFiles())
			{
				deleteDirectoryOrFile(f);
			}
		}
		
		directory.delete();
	}
}
