package frontend.templates;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import api.minecraft.MinecraftServer;
import attributes.Attributes;
import backend.api.GameServerFileDelete;
import backend.api.GameServerFileDeleteMultiple;
import backend.api.GameServerFileDownload;
import backend.api.GameServerFileRename;
import forms.Form;
import forms.TextField;
import frontend.GameServerFiles;
import frontend.javascript.JavascriptVariable;
import html.Element;
import server.GameServer;
import tags.Anchor;
import tags.Button;
import tags.Div;
import tags.H1;
import tags.HR;
import tags.LI;
import tags.Nav;
import tags.OL;
import tags.Script;
import tags.Span;
import util.Template;
import utils.Pair;
import utils.Utils;

public class GameServerFilesTemplate extends Template
{
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
	
	public GameServerFilesTemplate(int serverID, Class<? extends GameServer> serverType, String serverName, String directory, String[] directories, String redirectURL, List<Pair<String, Boolean>> files)
	{
		var main = Templates.getMainTemplate();
		var content = Div.class.cast(main.getBody().getElementById("content"));
		
		if(serverType.equals(MinecraftServer.class))
		{
			content.getStyles().setBackgroundImage("url('images/minecraft/files_background.jpg')");
		}
		
		content.addElements
		(
			new H1(serverName)
				.addClasses("ml-5", "mt-4", "text-light", "d-inline-block"),
			new Div()
				.addClasses("float-right", "mt-5", "mr-5")
				.addElements
				(
					Templates.generateConsoleLink(serverID).addClasses("mr-4"),
					Templates.generateFilesLink(serverID, serverName).addClasses("mr-4"),
					Templates.generateSettingsLink(serverID)
				),
			new HR(),
			new Nav() // Breadcrumb
				.addElements
				(
					new OL()
						.addClasses("breadcrumb")
						.addElements
						(
							IntStream.range(0, directories.length)
								 .mapToObj(i -> {
									 var breadcrumbItem = new LI().addClasses("breadcrumb-item");
									 
									 if(i == directories.length - 1)
									 {
										 breadcrumbItem.addClass("active");
										 breadcrumbItem.setData(directories[i]);
									 }
									 else
									 {
										 breadcrumbItem.addElement(new Anchor(directories[i], 
											Attributes.Href.makeAttribute(GameServerFiles.getEndpoint(serverID, String.join(",", Arrays.copyOf(directories, i+1)))))
										);
									 }
									 
									 return breadcrumbItem;
								 }).toArray(Element[]::new)
						)
				),
			new Div()
			.addClasses("d-flex", "flex-row", "mb-3")
			.addElements
			(
				new Button
					(
						Attributes.Title.makeAttribute("Upload files"),
						Attributes.OnClick.makeAttribute("upload('file')"),
						Attributes.makeAttribute("data-toggle", "tooltip"),
						Attributes.makeAttribute("data-placement", "top")
					)
					.addClasses("btn", "bg-light", "rounded", "ml-3")
					.addElements
					(
						Templates.createIcon("file-upload")
					),
				new Button
					(
						Attributes.Title.makeAttribute("Upload folder"),
						Attributes.OnClick.makeAttribute("upload('folder')"),
						Attributes.makeAttribute("data-toggle", "tooltip"),
						Attributes.makeAttribute("data-placement", "top")
					)
					.addClasses("btn", "bg-light", "rounded", "ml-3")
					.addElements
					(
						Templates.createIcon("upload")
					),
				new Anchor
					(
						Attributes.Title.makeAttribute("Download current folder"),
						Attributes.Href.makeAttribute(GameServerFileDownload.getEndpoint(serverID, directory)),
						Attributes.makeAttribute("data-toggle", "tooltip"),
						Attributes.makeAttribute("data-placement", "top")
					)
					.addClasses("btn", "bg-light", "rounded", "ml-3")
					.addElements
					(
						Templates.createIcon("download")
					),
				new Button
					(
						Attributes.Title.makeAttribute("Delete multiple files"),
						Attributes.OnClick.makeAttribute("startDeleteMultiple(this)"),
						Attributes.makeAttribute("data-toggle", "tooltip"),
						Attributes.makeAttribute("data-placement", "top")
					)
					.addClasses("btn", "bg-light", "rounded", "ml-3")
					.addElements
					(
						Templates.createIcon("fire")
					),
				new Button
					(
						Attributes.Title.makeAttribute("Submit delete"),
						Attributes.OnClick.makeAttribute("submitDeleteMultiple(this)"),
						Attributes.ID.makeAttribute("deleteMultipleSubmit"),
						Attributes.makeAttribute("data-toggle", "tooltip"),
						Attributes.makeAttribute("data-placement", "top"),
						Attributes.makeAttribute("link", Utils.encodeURL(
							GameServerFileDeleteMultiple.getEndpoint(serverID, String.join(",", directories), ""))),
						Attributes.Hidden.makeAttribute(true)
					)
					.addClasses("btn", "bg-success", "rounded", "ml-3")
					.addElements
					(
						Templates.createIcon("check")
					),
				new Button
					(
						Attributes.Title.makeAttribute("Create new folder"),
						Attributes.OnClick.makeAttribute("showFolderField()"),
						Attributes.makeAttribute("data-toggle", "tooltip"),
						Attributes.makeAttribute("data-placement", "top")
					)
					.addClasses("btn", "bg-light", "rounded", "ml-3")
					.addElements
					(
						Templates.createIcon("folder-plus")
					),
				new Div()
					.addClasses("form-inline")
					.addElements
					(
						new TextField
						(
							Attributes.Name.makeAttribute("folderName"),
							Attributes.ID.makeAttribute("folderName"),
							Attributes.Hidden.makeAttribute(true),
							Attributes.PlaceHolder.makeAttribute("Enter new folder name")
						)
						.addClasses("ml-3", "form-control"),
						new Button
						(
							"Submit",
							Attributes.Hidden.makeAttribute(true),
							Attributes.ID.makeAttribute("submitFolder"),
							Attributes.OnClick.makeAttribute(String.format("submitFolder('%s')", 
								GameServerFileRename.getEndpoint(serverID, directory, "", false)))
						)
						.addClasses("btn", "btn-primary", "form-control", "ml-3")
					)
			),
			new Div(Attributes.ID.makeAttribute("fileListing"))
				.addClasses("list-group", "list-group-flush")
				.addElements
				(
					files.stream()
					 	 .map(file -> {
					 		 var name = file.getFirst();
					 		 var isDirectory = file.getSecond();
					 		 var fileHref = GameServerFileDownload.getEndpoint(serverID, String.format("%s,%s", directory, name));
					 		 if(isDirectory)
					 		 {
					 			fileHref = GameServerFiles.getEndpoint(serverID, String.format("%s,%s", directory, name));
					 		 }
					 		 
					 		 var iconType = FILE_TYPES_FONT_AWESOME.getOrDefault(name.substring(name.lastIndexOf('.') + 1), "file");
					 		 
					 		 return new Div()
					 			.addClasses("d-flex", "justify-content-between")
					 			.addElements
					 			(
					 				new Anchor(Attributes.ID.makeAttribute("file-" + name),
					 					Attributes.Href.makeAttribute(fileHref))
					 					.addClasses("list-group-item", "list-group-item-action", "bg-dark", "text-light", "form-inline", "input-group", "input-group-sm", "col-lg-11")
					 					.addElements
					 					(
					 						(isDirectory ? Templates.createIcon("folder")   : iconType.equals("file")
					 									 ? Templates.createIcon("far", "file") : Templates.createIcon("far", "file-" + iconType))
					 							.addClasses("mr-2"),
					 						new Span(name)
					 					),
					 				new Button
						 				(
						 					Attributes.OnClick.makeAttribute(String.format("editName('%s', this)", name)),
						 					Attributes.Style.makeAttribute("max-width: 5%;"),
						 					Attributes.makeAttribute("linkLocation", fileHref),
						 					Attributes.makeAttribute("location", GameServerFileRename.getEndpoint(serverID, String.format("%s,%s", directory, name).replace(" ", "+"), "", false))
						 				)
						 				.addClasses("list-group-item", "list-group-item-action", "bg-warning", "text-light")
						 				.addElements
						 				(
						 					Templates.createIcon("edit")
						 				),
						 			new Button
						 			(
						 				Attributes.OnClick.makeAttribute("deleteFile(this)"),
						 				Attributes.Style.makeAttribute("max-width: 5%;"),
						 				Attributes.makeAttribute("fileName", name),
						 				Attributes.makeAttribute("location", GameServerFileDelete.getEndpoint(serverID, String.format("%s,%s", directory, name).replace(" ", "+")))
						 			)
						 			.addClasses("list-group-item", "list-group-item-action", "bg-danger", "text-light")
						 			.addElements
						 			(
						 				Templates.createIcon("trash")
						 			)
					 			);
					 	 }).toArray(Element[]::new)
				),
				new HR(),
				new Form
				(
					Attributes.Method.makeAttribute("POST"),
					Attributes.ID.makeAttribute("form"),
					Attributes.Enctype.makeAttribute("multipart/form-data")
				)
				.addElements
				(
					new forms.File
					(
						Attributes.Multiple.makeAttribute(true),
						Attributes.Hidden.makeAttribute(true),
						Attributes.ID.makeAttribute("fileUpload"),
						Attributes.Name.makeAttribute("fileUpload")
					)
				),
				new Form
				(
					Attributes.Method.makeAttribute("POST"),
					Attributes.ID.makeAttribute("zipForm"),
					Attributes.Enctype.makeAttribute("multipart/form-data"),
					Attributes.Action.makeAttribute(redirectURL + "&folder=true")
				)
				.addElements
				(
					new forms.File
					(
						Attributes.Hidden.makeAttribute(true),
						Attributes.ID.makeAttribute("zipUpload"),
						Attributes.Name.makeAttribute("zipUpload"),
						Attributes.Accept.makeAttribute(".zip")
					)
				)
		);
		
		var serverNameVariable = new JavascriptVariable<String>("name", serverName);
		var directoryVariable = new JavascriptVariable<String>("directory", directory);
		
		main.getBody().addEndElement(new Script(serverNameVariable.toString()));
		main.getBody().addEndElement(new Script(directoryVariable.toString()));
		main.getBody().addScript("js/GameServerFile.js");
		
		setBody(main.getBody());
		setHead(main.getHead());
	}
}
