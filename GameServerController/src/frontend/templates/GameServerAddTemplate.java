package frontend.templates;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import api.minecraft.MinecraftServer;
import attributes.Attributes;
import forms.Form;
import forms.TextField;
import model.Table;
import models.NodeTable;
import tags.Button;
import tags.Div;
import tags.H1;
import tags.HR;
import tags.Label;
import tags.ListItem;
import tags.Option;
import tags.Select;
import tags.Small;
import util.Template;

public class GameServerAddTemplate extends Template
{
	public GameServerAddTemplate(Pattern serverNamePattern, String[] nodeNames, List<Table> nodes, Map<String, Long> ramAmounts, Set<String> propertyNames)
	{
		var main = Templates.getMainTemplate();
		var content = Div.class.cast(main.getBody().getElementById("content"));
		content.getStyles().setBackgroundImage("url('images/material-back.jpeg')");
		
		content.addElements
		(
			new Div(
				Map.ofEntries(
					Map.entry(Attributes.ID.ATTRIBUTE_NAME, "inner-content")
				))
				.addClasses("ml-5", "mt-5")
				.addElements
				(
					new H1("Add Game Server").addClasses("text-light"),
					new HR().addClasses("border", "border-light"),
					new Form(
						Map.ofEntries(
							Map.entry(Attributes.Method.ATTRIBUTE_NAME, "POST"),
							Map.entry(Attributes.Enctype.ATTRIBUTE_NAME, "multipart/form-data")
						))
						.addElements
						(
							BootstrapTemplates.settingsInput(TextField.class, "Server Name", "name", "Enter server name", "", "This name must be unique. Cannot contain special characters.", true, false),
							BootstrapTemplates.settingsInput(TextField.class, "Enter Server Executable Name", "execName", "Enter executable name", "", "This will be the file executed when the server needs to start.", true, false),
							new ListItem()
								.addClasses("form-group", "form-inline", "list-group-item")
								.addElements
								(
									new Label("Select Node").addClasses("d-inline-block", "w-25", "align-middle"),
									new Select(
										Map.ofEntries(
											Map.entry(Attributes.ID.ATTRIBUTE_NAME, "node"),
											Map.entry(Attributes.Name.ATTRIBUTE_NAME, "node"),
											Map.entry(Attributes.Required.ATTRIBUTE_NAME, true),
											Map.entry("onchange", "changeNode()")
										))
										.addClasses("form-control")
										.addElements
										(
											Stream.of(nodeNames).map(nodeName -> {
												var option = new Option(nodeName);
												for(var node : nodes)
												{
													if(node.getColumnValue(NodeTable.NAME).equals(nodeName))
													{
														option.setAttribute("totalram", String.valueOf(node.getColumnValue(NodeTable.MAX_RAM_ALLOWED)));
														option.setAttribute("reservedram", ramAmounts.get(nodeName));
													}
												}
												return option;
											}).toArray(Option[]::new)
										),
									new Small().addClasses("form-text", "text-muted")
								),
							new ListItem()
								.addClasses("form-group", "form-inline", "list-group-item")
								.addElements
								(
									new Label("Select Server Type").addClasses("d-inline-block", "w-25", "align-middle"),
									new Select(
										Map.ofEntries(
											Map.entry(Attributes.ID.ATTRIBUTE_NAME, "type"),
											Map.entry(Attributes.Required.ATTRIBUTE_NAME, true),
											Map.entry(Attributes.Name.ATTRIBUTE_NAME, "type"),
											Map.entry("onchange", "changeType()")
										))
										.addClasses("form-control")
										.addElements
										(
											propertyNames.stream()
														 .map(name -> new Option(name))
														 .toArray(Option[]::new)
										)
								),
							BootstrapTemplates.settingsInput(forms.Number.class, "Maximum RAM (MB)", "ramAmount", "Enter maximum amount of ram to use in MB", String.valueOf(MinecraftServer.MINIMUM_HEAP_SIZE), "RAM must be in 1024 increments", true, false)
								.addClasses("minecraft-type"),
							BootstrapTemplates.settingsInput(forms.Checkbox.class, "Automatic Restart", "restartsUnexpected", null, "true", "This will restart the server when it stops unexpectingly", false, false)
								.addClasses("minecraft-type"),
							BootstrapTemplates.settingsInput(forms.File.class, "Starting Files", "files", null, "", "The zip given will unzip in the parent directory. This is optional", false, false),
							new ListItem()
								.addClasses("list-group-item")
								.addElements
								(
									new Button("Submit").addClasses("btn", "btn-primary")
								)
						)
				)
		);
		
		TextField.class.cast(content.getElementById("name")).setPattern(serverNamePattern.pattern());
		
		var heapInput = forms.Number.class.cast(content.getElementById("ramAmount"));
		heapInput.setMin((double) MinecraftServer.MINIMUM_HEAP_SIZE);
		heapInput.setStep((double) 1024);
		
		var fileInput = forms.File.class.cast(content.getElementById("files"));
		fileInput.removeClass("form-control");
		fileInput.addClass("form-control-file");
		fileInput.setAccept(".zip");
		
		main.getBody().addScript("js/addServer.js");
		
		setBody(main.getBody());
		setHead(main.getHead());
	}
}
