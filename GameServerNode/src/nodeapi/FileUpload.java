package nodeapi;

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

import nodemain.NodeProperties;
import utils.Utils;

@WebServlet(
		name = "FileUpload",
		urlPatterns = "/FileUpload",
		asyncSupported = true
)
@MultipartConfig
public class FileUpload extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var directory = ApiSettings.DIRECTORY.parse(request);
		var folder = ApiSettings.FOLDER.parse(request);
		
		if(!Utils.optionalsPresent(directory, folder))
		{
			response.setStatus(400);
			return;
		}
		
		var fileParts = request.getParts()
							   .parallelStream()
							   .filter(p -> p.getSubmittedFileName() != null)
							   .collect(Collectors.toList());
		
		for(var p : fileParts)
		{
			var fileName = p.getSubmittedFileName();
			
			var directoryPath = Paths.get(NodeProperties.DEPLOY_FOLDER, directory.get().toArray(String[]::new));
			
			if(folder != null && fileName.endsWith(".zip"))
			{
				uploadFolder(directoryPath.toFile(), p.getInputStream());
			}
			else
			{
				var newFile = directoryPath.resolve(fileName).toFile();
				if(!newFile.exists())
				{
					p.write(newFile.getAbsolutePath());
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
