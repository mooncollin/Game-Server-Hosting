package api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.NodeProperties;

@WebServlet("/FileUpload")
@MultipartConfig
public class FileUpload extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/FileUpload";
	
	public static String getEndpoint(String directory, boolean isFolder)
	{
		var folderString = "";
		if(isFolder)
		{
			folderString = "&folder=true";
		}
		
		return String.format("%s?directory=%s%s", URL, directory, folderString);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setStatus(400);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var directory = request.getParameter("directory");
		var folder = request.getParameter("folder");
		
		if(directory == null)
		{
			response.setStatus(400);
			return;
		}
		
		var directories = directory.split(",");
		
		var fileParts = request.getParts()
							   .parallelStream()
							   .filter(p -> p.getSubmittedFileName() != null)
							   .collect(Collectors.toList());
		
		for(var p : fileParts)
		{
			var fileName = p.getSubmittedFileName();
			
			var directoryPath = Paths.get(NodeProperties.DEPLOY_FOLDER, directories);
			
			if(folder != null && fileName.endsWith(".zip"))
			{
				uploadFolder(directoryPath.toFile(), p.getInputStream());
			}
			else
			{
				var newFile = directoryPath.resolve(fileName).toFile();
				if(!newFile.exists())
				{
					try(var s = new FileOutputStream(newFile))
					{
						p.getInputStream().transferTo(s);
					}
				}
			}
		}
	}
	
	public static void uploadFolder(File directory, InputStream data) throws IOException
	{
		try(var zippy = new ZipInputStream(data))
		{
			ZipEntry entry;
			while((entry = zippy.getNextEntry()) != null)
			{
				var currentFile = directory.toPath().resolve(entry.getName()).toFile();
				if(!currentFile.getParentFile().exists())
				{
					currentFile.getParentFile().mkdirs();
				}
				if(!entry.isDirectory())
				{
					try(var s = new FileOutputStream(currentFile))
					{
						zippy.transferTo(s);
					}
				}
				zippy.closeEntry();
			}
		}
	}
}
