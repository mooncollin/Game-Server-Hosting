package nodeapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nodemain.NodeProperties;

@WebServlet(
		name = "FileDownload",
		urlPatterns = "/FileDownload",
		asyncSupported = true
)
public class FileDownload extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var directory = ApiSettings.DIRECTORY.parse(request);
		if(directory.isEmpty())
		{
			response.setStatus(400);
			return;
		}
		
		var currentFile = Paths.get(NodeProperties.DEPLOY_FOLDER, directory.get().toArray(String[]::new)).toFile();
		if(!currentFile.exists())
		{
			response.setStatus(400);
			return;
		}
		
		if(currentFile.isFile())
		{
			try(var s = new FileInputStream(currentFile))
			{
				s.transferTo(response.getOutputStream());
			}
		}
		else if(currentFile.isDirectory())
		{
			try(var s = new ZipOutputStream(response.getOutputStream()))
			{
				for(var file : currentFile.listFiles())
				{
					zipDirectory(null, file, s);
				}
			}
		}
	}
	
	private void zipDirectory(Path currentFolder, File currentFile, ZipOutputStream zipOut) throws IOException
	{
		var parentName = currentFolder == null ? "" : currentFolder.toString();
		if(currentFile.isFile())
		{
			var parent = Paths.get(parentName, currentFile.getName());
			zipOut.putNextEntry(new ZipEntry(parent.toString()));
			try(var fileIn = new FileInputStream(currentFile))
			{
				fileIn.transferTo(zipOut);
			}
			zipOut.closeEntry();
		}
		else
		{
			var parent = Paths.get(parentName, currentFile.getName());
			for(var file : currentFile.listFiles())
			{
				zipDirectory(parent, file, zipOut);
			}
		}
	}
}
