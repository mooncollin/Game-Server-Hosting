package api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import main.NodeProperties;

@WebServlet("/FileUpload")
@MultipartConfig
public class FileUpload extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setStatus(400);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String directory = request.getParameter("directory");
		if(directory == null)
		{
			response.setStatus(400);
			return;
		}
		
		String[] directories = directory.split(",");
		
		for(Part p : request.getParts())
		{
			String header = p.getHeader("Content-Disposition");
			String fileName = header.substring(header.indexOf("filename=") + "filename=".length() + 1);
			fileName = fileName.substring(0, fileName.length() - 1);
			Path directoryPath = Paths.get(NodeProperties.DEPLOY_FOLDER, directories);
			
			if(request.getParameter("folder") != null && fileName.endsWith(".zip"))
			{
				uploadFolder(directoryPath.toFile(), p.getInputStream());
			}
			else
			{
				File newFile = directoryPath.resolve(fileName).toFile();
				if(!newFile.exists())
				{
					try(FileOutputStream s = new FileOutputStream(newFile))
					{
						p.getInputStream().transferTo(s);
					}
				}
			}
		}
	}
	
	public static void uploadFolder(File directory, InputStream data) throws IOException
	{
		try(ZipInputStream zippy = new ZipInputStream(data))
		{
			ZipEntry entry;
			while((entry = zippy.getNextEntry()) != null)
			{
				File currentFile = directory.toPath().resolve(entry.getName()).toFile();
				if(!currentFile.getParentFile().exists())
				{
					currentFile.getParentFile().mkdirs();
				}
				if(!entry.isDirectory())
				{
					try(FileOutputStream s = new FileOutputStream(currentFile))
					{
						zippy.transferTo(s);
					}
				}
				zippy.closeEntry();
			}
		}
	}
}
