package nodeapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nodemain.NodeProperties;
import utils.ParameterURL;

@WebServlet("/FileDownload")
public class FileDownload extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/FileDownload";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		ParameterURL.HTTP_PROTOCOL, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL getEndpoint(String directory)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.DIRECTORY_PARAMETER, directory);
		return url;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var directory = request.getParameter(ApiSettings.DIRECTORY_PARAMETER);
		if(directory == null || directory.isEmpty())
		{
			response.setStatus(400);
			return;
		}
		
		var directories = directory.split(",");
		
		var currentFile = Paths.get(NodeProperties.DEPLOY_FOLDER, directories).toFile();
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
				for(var deeperFile : currentFile.listFiles())
				{
					zipDirectory(currentFile.getName(), deeperFile, s);
				}
			}
		}
	}
	
	private void zipDirectory(String currentDirectory, File currentFile, ZipOutputStream zipOut) throws IOException
	{
		if(currentFile.isFile())
		{
			var fileName = String.format("%s%s%s", currentDirectory, File.separator, currentFile.getName());
			zipOut.putNextEntry(new ZipEntry(fileName));
			try(var fileIn = new FileInputStream(currentFile))
			{
				fileIn.transferTo(zipOut);
			}
			zipOut.closeEntry();
		}
		else if(currentFile.isDirectory())
		{
			for(var deeperFile : currentFile.listFiles())
			{
				var directoryName = currentFile.getName();
				if(!deeperFile.getName().equals(currentDirectory))
				{
					directoryName = String.format("%s%s%s", currentDirectory, File.separator, currentFile.getName());
				}
				zipDirectory(directoryName, deeperFile, zipOut);
			}
		}
	}
}
