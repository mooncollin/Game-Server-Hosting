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

@WebServlet("/FileDelete")
public class FileDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/FileDelete";
	
	public static String getEndpoint(String directory)
	{
		return String.format("%s?directory=%s", URL, directory);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var directory = request.getParameter("directory");
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
