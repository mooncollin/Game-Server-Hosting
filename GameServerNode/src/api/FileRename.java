package api;

import java.io.File;
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

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String directory = request.getParameter("directory");
		String rename = request.getParameter("rename");
		String newFolder = request.getParameter("newFolder");
		if((directory == null || (rename == null && newFolder == null)) && !newFolder.isBlank())
		{
			response.setStatus(400);
			return;
		}
		
		String[] directories = directory.split(",");
		
		File currentFile = Paths.get(NodeProperties.DEPLOY_FOLDER, directories).toFile();
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
