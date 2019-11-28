package api;

import java.io.IOException;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.NodeProperties;

@WebServlet("/FileRename")
public class FileRename extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/FileRename";
	
	public static String getEndpoint(String directory, String rename, boolean newFolder)
	{
		return String.format("%s?directory=%s&%s=%s", URL, directory, 
			newFolder ? "newFolder" : "rename", rename);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var directory = request.getParameter("directory");
		var rename = request.getParameter("rename");
		var newFolder = request.getParameter("newFolder");
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
