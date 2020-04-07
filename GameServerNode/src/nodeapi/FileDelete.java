package nodeapi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nodemain.NodeProperties;
import utils.servlet.Endpoint;
import utils.servlet.ParameterURL;

@WebServlet("/FileDelete")
public class FileDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/FileDelete";
	
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
		if(directory.isEmpty())
		{
			response.setStatus(400);
			return;
		}
		
		var currentFile = Paths.get(NodeProperties.DEPLOY_FOLDER, directory.get().toArray(String[]::new)).toFile();
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
			Arrays.stream(directory.listFiles())
				  .parallel()
				  .forEach(FileDelete::deleteDirectoryOrFile);
		}
		
		directory.delete();
	}
}
