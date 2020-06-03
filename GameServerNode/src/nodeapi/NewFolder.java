package nodeapi;

import java.io.IOException;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nodemain.NodeProperties;

@WebServlet(
		name = "NewFolder",
		urlPatterns = "/NewFolder",
		asyncSupported = true
)
public class NewFolder extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var directory = ApiSettings.DIRECTORY.parse(request);
		var newFolder = ApiSettings.NEW_FOLDER.parse(request);
		if(directory.isEmpty() || newFolder.isEmpty())
		{
			response.setStatus(400);
			return;
		}
		
		var currentFile = Paths.get(NodeProperties.DEPLOY_FOLDER, directory.get().toArray(String[]::new)).resolve(newFolder.get()).toFile();
		
		if(currentFile.equals(Paths.get(NodeProperties.DEPLOY_FOLDER).toFile()))
		{
			response.setStatus(400);
			return;
		}
		
		if(!currentFile.exists())
		{
			currentFile.mkdirs();
		}
	}
}
