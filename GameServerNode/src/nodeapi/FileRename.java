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
import utils.servlet.Endpoint;
import utils.servlet.ParameterURL;

@WebServlet("/FileRename")
public class FileRename extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/FileRename";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
			Endpoint.Protocol.HTTP, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL getEndpoint(List<String> directories, String rename, boolean newFolder)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
		url.addQuery(newFolder ? ApiSettings.NEW_FOLDER.getName() : ApiSettings.RENAME.getName(), rename);
		return url;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var directory = ApiSettings.DIRECTORY.parse(request);
		var rename = ApiSettings.RENAME.parse(request);
		var newFolder = ApiSettings.NEW_FOLDER.parse(request);
		if((directory.isEmpty() || (rename.isEmpty() && newFolder.isEmpty())))
		{
			response.setStatus(400);
			return;
		}
		
		var currentFile = Paths.get(NodeProperties.DEPLOY_FOLDER, directory.get().toArray(String[]::new)).toFile();
		if(currentFile.equals(Paths.get(NodeProperties.DEPLOY_FOLDER).toFile()))
		{
			response.setStatus(400);
			return;
		}
		
		if(newFolder.isPresent())
		{
			currentFile = Paths.get(currentFile.getAbsolutePath(), newFolder.get()).toFile();
			currentFile.mkdir();
		}
		else if(currentFile.exists())
		{
			currentFile.renameTo(Paths.get(currentFile.getParent(), rename.get()).toFile());	
		}
	}
}
