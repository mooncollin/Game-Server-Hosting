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
		name = "FileRename",
		urlPatterns = "/FileRename",
		asyncSupported = true
)
public class FileRename extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var directory = ApiSettings.DIRECTORY.parse(request);
		var rename = ApiSettings.RENAME.parse(request);
		if(directory.isEmpty() || rename.isEmpty())
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
		
		if(currentFile.exists())
		{
			currentFile.renameTo(Paths.get(currentFile.getParent(), rename.get()).toFile());	
		}
	}
}
