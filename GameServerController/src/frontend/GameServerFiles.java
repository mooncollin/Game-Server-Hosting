package frontend;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import api.minecraft.MinecraftServer;
import backend.api.GameServerFileDelete;
import backend.api.GameServerFileDeleteMultiple;
import backend.api.GameServerFileDownload;
import backend.api.GameServerFileRename;
import backend.api.ServerInteract;
import backend.main.StartUpApplication;
import forms.Form;
import forms.TextField;
import frontend.templates.Templates;
import html.CompoundElement;
import html.Element;
import tags.Anchor;
import tags.Button;
import tags.Script;
import util.Template;
import utils.Utils;

@WebServlet("/GameServerFiles")
@MultipartConfig
public class GameServerFiles extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerFiles";

	public static final Map<String, String> FILE_TYPES_FONT_AWESOME = Map.ofEntries
	(
			Map.entry("zip", "archive"),
			Map.entry("tar", "archive"),
			Map.entry("gz", "archive"),
			Map.entry("rar", "archive"),
			Map.entry("jar", "archive"),
			Map.entry("mp3", "audio"),
			Map.entry("ogg", "audio"),
			Map.entry("wav", "audio"),
			Map.entry("py", "code"),
			Map.entry("java", "code"),
			Map.entry("c", "code"),
			Map.entry("cpp", "code"),
			Map.entry("lua", "code"),
			Map.entry("pl", "code"),
			Map.entry("html", "code"),
			Map.entry("css", "code"),
			Map.entry("js", "code"),
			Map.entry("xml", "code"),
			Map.entry("md", "code"),
			Map.entry("json", "code"),
			Map.entry("jpg", "image"),
			Map.entry("jpeg", "image"),
			Map.entry("png", "image"),
			Map.entry("svg", "image"),
			Map.entry("mp4", "movie"),
			Map.entry("ppt", "powerpoint"),
			Map.entry("pptx", "powerpoint"),
			Map.entry("doc", "word"),
			Map.entry("docx", "word"),
			Map.entry("txt", "alt"),
			Map.entry("properties", "alt"),
			Map.entry("log", "alt")
	);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
		String directory = request.getParameter("directory");
		String redirectURL = request.getRequestURL() + "?" + request.getQueryString().replace("&folder=true", "");
		if(serverName == null || directory == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var serverFound = StartUpApplication.getServerInfo().get(serverName);
		if(serverFound == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		if(directory.isEmpty())
		{
			directory = serverName;
		}
		
		String[] directories = directory.split(",");
		
		String deployFolder;
		
		try
		{
			String url = "http://" + serverFound.getSecond() + "/NodeInfo?name=" + serverName.replace(' ', '+') + "&property=deploy_folder";
			HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url)).build();
			HttpResponse<String> httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.ofString());
			if(httpResponse.statusCode() != 200)
			{
				response.setStatus(500);
				return;
			}
			
			deployFolder = httpResponse.body();
		}
		catch(InterruptedException e)
		{
			response.setStatus(500);
			return;
		}
		
		File currentDirectory = Paths.get(deployFolder, directories).toFile();
		if(!currentDirectory.isDirectory())
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		Template template = Templates.getMainTemplate();
		CompoundElement content = (CompoundElement) template.getBody().getElementById("content");
		if(serverFound.getFirst().equals(MinecraftServer.class))
		{
			content.setStyle("background-image: url('images/minecraft/files_background.jpg')");
		}
		
		CompoundElement header = new CompoundElement("h1", serverName);
		header.addClasses("ml-5", "mt-4", "text-light", "d-inline-block");
		content.addElement(header);
		
		CompoundElement linksContainer = new CompoundElement("div");
		linksContainer.addClasses("float-right", "mt-5", "mr-5");
		
		Anchor consoleLink = Templates.generateConsoleLink(serverName);
		consoleLink.addClasses("mr-4");
		Anchor filesLink = Templates.generateFilesLink(serverName);
		filesLink.addClasses("mr-4");
		Anchor settingsLink = Templates.generateSettingsLink(serverName);
		
		linksContainer.addElement(consoleLink);
		linksContainer.addElement(filesLink);
		linksContainer.addElement(settingsLink);
		
	
		content.addElement(linksContainer);
		content.addElement(new Element("hr"));
		
		CompoundElement breadcrumb = new CompoundElement("nav");
		CompoundElement directoryList = new CompoundElement("ol");
		directoryList.addClass("breadcrumb");
		
		for(int i = 0; i < directories.length; i++)
		{
			CompoundElement breadcrumbItem = new CompoundElement("li");
			breadcrumbItem.addClass("breadcrumb-item");
			if(i == directories.length - 1)
			{
				breadcrumbItem.addClass("active");
				breadcrumbItem.setData(directories[i]);
			}
			else
			{
				Anchor breadcrumbLink = new Anchor(directories[i]);
				breadcrumbLink.setHref(URL + "?name=" + serverName + "&directory=" + String.join(",", Arrays.copyOf(directories, i+1)));
				breadcrumbItem.addElement(breadcrumbLink);
			}
			
			directoryList.addElement(breadcrumbItem);
		}
		
		breadcrumb.addElement(directoryList);
		content.addElement(breadcrumb);
		
		CompoundElement optionsList = new CompoundElement("div");
		optionsList.addClasses("d-flex", "flex-row", "mb-3");
		
		Button uploadFilesButton = new Button();
		uploadFilesButton.addClasses("btn", "bg-light", "rounded", "ml-3");
		uploadFilesButton.setAttribute("data-toggle", "tooltip");
		uploadFilesButton.setAttribute("data-placement", "top");
		uploadFilesButton.setTitle("Upload files");
		uploadFilesButton.setOnClick("upload('file')");
		
		CompoundElement uploadFilesIcon = new CompoundElement("i");
		uploadFilesIcon.addClasses("fas", "fa-file-upload");
		uploadFilesButton.addElement(uploadFilesIcon);
		
		optionsList.addElement(uploadFilesButton);
		
		Button uploadZipButton = new Button();
		uploadZipButton.addClasses("btn", "bg-light", "rounded", "ml-3");
		uploadZipButton.setAttribute("data-toggle", "tooltip");
		uploadZipButton.setAttribute("data-placement", "top");
		uploadZipButton.setTitle("Upload folder");
		uploadZipButton.setOnClick("upload('folder')");
		
		CompoundElement zipIcon = new CompoundElement("i");
		zipIcon.addClasses("fas", "fa-upload");
		uploadZipButton.addElement(zipIcon);
		
		optionsList.addElement(uploadZipButton);
		
		Anchor downloadFolder = new Anchor();
		downloadFolder.addClasses("btn", "bg-light", "rounded", "ml-3");
		downloadFolder.setAttribute("data-toggle", "tooltip");
		downloadFolder.setAttribute("data-placement", "top");
		downloadFolder.setTitle("Download current folder");
		downloadFolder.setHref(String.format("%s?name=%s&directory=%s", GameServerFileDownload.URL, serverName, directory));
		
		CompoundElement downloadFolderIcon = new CompoundElement("i");
		downloadFolderIcon.addClasses("fas", "fa-download");
		downloadFolder.addElement(downloadFolderIcon);
		
		optionsList.addElement(downloadFolder);
		
		Button deleteMultipleButton = new Button();
		deleteMultipleButton.addClasses("btn", "bg-light", "rounded", "ml-3");
		deleteMultipleButton.setAttribute("data-toggle", "tooltip");
		deleteMultipleButton.setAttribute("data-placement", "top");
		deleteMultipleButton.setTitle("Delete multiple files");
		deleteMultipleButton.setOnClick("startDeleteMultiple(this)");
		
		CompoundElement deleteMultipleIcon = new CompoundElement("i");
		deleteMultipleIcon.addClasses("fas", "fa-fire");
		deleteMultipleButton.addElement(deleteMultipleIcon);
		
		optionsList.addElement(deleteMultipleButton);
		
		Button deleteMultipleSubmit = new Button();
		deleteMultipleSubmit.addClasses("btn", "bg-success", "rounded", "ml-3");
		deleteMultipleSubmit.setAttribute("data-toggle", "tooltip");
		deleteMultipleSubmit.setAttribute("data-placement", "top");
		deleteMultipleSubmit.setTitle("Submit delete");
		deleteMultipleSubmit.setOnClick("submitDeleteMultiple(this)");
		deleteMultipleSubmit.setID("deleteMultipleSubmit");
		deleteMultipleSubmit.setAttribute("link", Utils.encodeURL(String.format("%s?name=%s&directory=%s", GameServerFileDeleteMultiple.URL, serverName, String.join(",", directories))));
		deleteMultipleSubmit.setHidden(true);
		
		CompoundElement deleteMultipleSubmitIcon = new CompoundElement("i");
		deleteMultipleSubmitIcon.addClasses("fas", "fa-check");
		deleteMultipleSubmit.addElement(deleteMultipleSubmitIcon);
		
		optionsList.addElement(deleteMultipleSubmit);
		
		Button makeFolderButton = new Button();
		makeFolderButton.addClasses("btn", "bg-light", "rounded", "ml-3");
		makeFolderButton.setAttribute("data-toggle", "tooltip");
		makeFolderButton.setAttribute("data-placement", "top");
		makeFolderButton.setTitle("Create new folder");
		makeFolderButton.setOnClick("showFolderField()");
		
		CompoundElement makeFolderIcon = new CompoundElement("i");
		makeFolderIcon.addClasses("fas", "fa-folder-plus");
		makeFolderButton.addElement(makeFolderIcon);
		
		TextField folderName = new TextField();
		folderName.setName("folderName");
		folderName.setID("folderName");
		folderName.setHidden(true);
		folderName.addClasses("ml-3", "form-control");
		folderName.setPlaceHolder("Enter new folder name");
		
		optionsList.addElement(makeFolderButton);
		
		CompoundElement submit = new CompoundElement("button", "Submit");
		submit.addClasses("btn", "btn-primary", "form-control", "ml-3");
		submit.setHidden(true);
		submit.setOnClick(String.format("submitFolder('%s')", String.format("%s?name=%s&directory=%s", GameServerFileRename.URL, serverName, directory)));
		submit.setID("submitFolder");
		
		CompoundElement folderFormGroup = new CompoundElement("div");
		folderFormGroup.addClasses("form-inline");
		folderFormGroup.addElement(folderName);
		folderFormGroup.addElement(submit);
		
		optionsList.addElement(folderFormGroup);
		
		Form f = new Form();
		f.setMethod("POST");
		f.setID("form");
		f.setEnctype("multipart/form-data");
		
		forms.File fileInput = new forms.File();
		fileInput.setMultiple(true);
		fileInput.setHidden(true);
		fileInput.setID("fileUpload");
		fileInput.setName("fileUpload");
		
		forms.File zipInput = new forms.File();
		zipInput.setHidden(true);
		zipInput.setID("zipUpload");
		zipInput.setName("zipUpload");
		zipInput.addAccept(".zip");
		
		Form zipForm = new Form();
		zipForm.setMethod("POST");
		zipForm.setID("zipForm");
		zipForm.setEnctype("multipart/form-data");
		zipForm.setAction(redirectURL + "&folder=true");
		
		f.addInput(fileInput);
		zipForm.addInput(zipInput);
		content.addEndElement(f);
		content.addEndElement(zipForm);
		
		CompoundElement fileListing = new CompoundElement("div");
		fileListing.addClasses("list-group", "list-group-flush");
		fileListing.setID("fileListing");
		
		File[] files = currentDirectory.listFiles();
		
		for(File file : files)
		{
			Anchor fileItem = new Anchor();
			fileItem.addClasses("list-group-item", "list-group-item-action", "bg-dark", "text-light", "form-inline", "input-group", "input-group-sm");
			fileItem.addClass("col-lg-11");
			fileItem.setID("file-" + file.getName());
			CompoundElement fileIcon = new CompoundElement("i");
			fileIcon.addClasses("mr-2");
			CompoundElement fileName = new CompoundElement("span", file.getName());
			String fileHref;
			if(file.isDirectory())
			{
				fileIcon.addClasses("fas", "fa-folder");
				fileHref = String.format("%s?name=%s&directory=%s,%s", URL, serverName, directory, file.getName());
			}
			else
			{
				String iconType = FILE_TYPES_FONT_AWESOME.getOrDefault(file.getName().substring(file.getName().lastIndexOf('.') + 1), "file");
				if(iconType.equals("file"))
				{
					fileIcon.addClasses("far", "fa-file");
				}
				else
				{
					fileIcon.addClasses("far", "fa-file-" + iconType);
				}
				fileHref = String.format("%s?name=%s&directory=%s,%s", GameServerFileDownload.URL, serverName, directory, file.getName());
			}
			fileItem.setHref(fileHref);
			
			fileItem.addElement(fileIcon);
			fileItem.addElement(fileName);
			
			Button editButton = new Button();
			editButton.addClasses("list-group-item", "list-group-item-action", "bg-warning", "text-light");
			editButton.setStyle("max-width: 5%;");
			editButton.setOnClick(String.format("editName('%s', this)", file.getName()));
			editButton.setAttribute("location", String.format("%s?name=%s&directory=%s,%s", GameServerFileRename.URL, serverName, directory, file.getName()).replace(" ", "+"));
			editButton.setAttribute("linkLocation", fileHref);
			CompoundElement editIcon = new CompoundElement("i");
			editIcon.addClasses("fas", "fa-edit");
			
			editButton.addElement(editIcon);
			
			Button deleteButton = new Button();
			deleteButton.addClasses("list-group-item", "list-group-item-action", "bg-danger", "text-light");
			deleteButton.setStyle("max-width: 5%;");
			deleteButton.setAttribute("location", String.format("%s?name=%s&directory=%s,%s", GameServerFileDelete.URL, serverName, directory, file.getName()).replace(" ", "+"));
			deleteButton.setAttribute("fileName", file.getName());
			deleteButton.setOnClick("deleteFile(this)");
			CompoundElement deleteIcon = new CompoundElement("i");
			deleteIcon.addClasses("fas", "fa-trash");
			
			deleteButton.addElement(deleteIcon);
			
			CompoundElement fileRow = new CompoundElement("div");
			fileRow.addClasses("d-flex", "justify-content-between");
			
			fileRow.addElement(fileItem);
			fileRow.addElement(editButton);
			fileRow.addElement(deleteButton);
			
			fileListing.addElement(fileRow);
		}
		
		content.addElement(optionsList);
		content.addElement(fileListing);
		content.addElement(new Element("hr"));
		template.getBody().addEndElement(new Script(String.format("var name='%s'; var directory='%s';", serverName, directory)));
		template.getBody().addScript("js/GameServerFile.js");
		response.setContentType("text/html");
		response.getWriter().print(template);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		
		String name = request.getParameter("name");
		String directory = request.getParameter("directory");
		
		if(name == null || directory == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		String redirectURL = request.getRequestURL() + "?" + request.getQueryString().replace("&folder=true", "");
		var node = StartUpApplication.getServerInfo().get(name);
		ByteArrayOutputStream fullRequest = new ByteArrayOutputStream();
		String url = "http://" + node.getSecond() + "/FileUpload?name=" + name.replace(' ', '+') + "&directory=" + directory.replace(' ', '+');
		final String boundary = "===" + System.currentTimeMillis() + "===";
		
		if(request.getParameter("folder") != null)
		{
			url += "&folder=true";
		}
		
		
		for(Part p : request.getParts())
		{
			InputStream stream = p.getInputStream();
			
			fullRequest.writeBytes(String.format("\r\n--%s\r\nContent-Disposition: %s\r\n\r\n", boundary, p.getHeader("Content-Disposition")).getBytes());
			stream.transferTo(fullRequest);
		}
		
		fullRequest.writeBytes(String.format("\r\n--%s--", boundary).getBytes());
		
		HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
				.header("Content-type", "multipart/form-data; boundary=" + boundary)
				.POST(BodyPublishers.ofByteArray(fullRequest.toByteArray()))
				.build();
		try
		{
			ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
		} catch (InterruptedException e)
		{
		}
		
		response.sendRedirect(redirectURL);
	}
}
