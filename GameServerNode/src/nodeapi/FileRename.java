package nodeapi;

import java.io.IOException;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nodemain.NodeProperties;
import utils.ParameterURL;

@WebServlet("/FileRename")
public class FileRename extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/FileRename";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		ParameterURL.HTTP_PROTOCOL, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL getEndpoint(String directory, String rename, boolean newFolder)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.DIRECTORY_PARAMETER, directory);
		url.addQuery(newFolder ? ApiSettings.NEW_FOLDER_PARAMETER : ApiSettings.RENAME_PARAMETER, rename);
		return url;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var directory = request.getParameter(ApiSettings.DIRECTORY_PARAMETER);
		var rename = request.getParameter(ApiSettings.RENAME_PARAMETER);
		var newFolder = request.getParameter(ApiSettings.NEW_FOLDER_PARAMETER);
		if((directory == null || (rename == null && newFolder == null)) && !newFolder.isBlank())
		{
			response.setStatus(400);
			return;
		}
		
		var directories = directory.split(",");
		
		var currentFile = Paths.get(NodeProperties.DEPLOY_FOLDER, directories).toFile();
		if(currentFile.equals(Paths.get(NodeProperties.DEPLOY_FOLDER).toFile()))
		{
			response.setStatus(400);
			return;
		}
		
		if(newFolder != null)
		{
			currentFile = Paths.get(currentFile.getAbsolutePath(), newFolder).toFile();
			currentFile.mkdir();
		}
		else if(currentFile.exists())
		{
			currentFile.renameTo(Paths.get(currentFile.getParent(), rename).toFile());	
		}
	}
}
