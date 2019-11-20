package frontend.templates;

import java.util.Map;

import api.minecraft.MinecraftServer;
import attributes.Attributes;
import forms.Form;
import forms.Input;
import forms.TextField;
import html.CompoundElement;
import model.Table;
import models.GameServerTable;
import models.MinecraftServerTable;
import server.GameServer;
import tags.Button;
import tags.Div;
import tags.H1;
import tags.HR;
import tags.Icon;
import tags.LI;
import tags.Small;
import tags.UL;
import util.Template;

public class GameServerSettingsTemplate extends Template
{
	public GameServerSettingsTemplate(String serverName, Class<? extends GameServer> serverClass, Table gameServer, Table minecraftServer, long totalRam, long reservedRam, Map<String, Object> properties)
	{
		var main = Templates.getMainTemplate();
		var content = Div.class.cast(main.getBody().getElementById("content"));
		
		if(serverClass.equals(MinecraftServer.class))
		{
			content.getStyles().setBackgroundImage("url('images/minecraft/settings_background.jpg')");
		}
		
		content.addElements
		(
			new H1(serverName).addClasses("text-light", "ml-5", "mt-4", "d-inline-block"),
			new Div()
				.addClasses("float-right", "mt-5", "mr-5")
				.addElements
				(
					Templates.generateConsoleLink(serverName).addClasses("mr-4"),
					Templates.generateFilesLink(serverName).addClasses("mr-4"),
					Templates.generateSettingsLink(serverName)
				),
			new HR(),
			new UL() // Options List
			.addClasses("list-group", "list-group-flush", "float-right", "sticky-top")
			.addElements
			(
				new LI()
					.addClasses("list-group-item", "form-group", "rounded", "mr-5")
					.addElements
					(
						new Button(
							Map.ofEntries(
								Map.entry(Attributes.OnClick.ATTRIBUTE_NAME, "edit()"),
								Map.entry(Attributes.ID.ATTRIBUTE_NAME, "editButton"),
								Map.entry(Attributes.Type.ATTRIBUTE_NAME, "button")
							))
							.addClasses("btn", "btn-warning")
							.addElements
							(
								new Icon().addClasses("fas", "fa-pencil-alt")
							),
						new Button(
							Map.ofEntries(
								Map.entry(Attributes.ID.ATTRIBUTE_NAME, "submitButton"),
								Map.entry(Attributes.Hidden.ATTRIBUTE_NAME, true),
								Map.entry(Attributes.Form.ATTRIBUTE_NAME, "settingsForm")
							))
							.addClasses("btn", "btn-success", "ml-3")
							.addElements
							(
								new Icon().addClasses("fas", "fa-check")
							)
					)
			),
			new Div()
				.addClasses("container", "bg-dark")
				.addElements
				(
					new Form(
						Map.ofEntries(
							Map.entry(Attributes.Method.ATTRIBUTE_NAME, "POST"),
							Map.entry(Attributes.ID.ATTRIBUTE_NAME, "settingsForm")
						))
						.addElements
						(
							new UL() // Settings List
								.addClasses("list-group", "list-group-flush")
								.addElements
								(
									new LI()
										.addClasses("list-group-item")
										.addElements
										(
											new H1("Settings")
										),
									BootstrapTemplates.settingsInput(TextField.class, "Executable Name", "execName", "Enter name of server executable", gameServer.getColumnValue(GameServerTable.EXECUTABLE_NAME), null, true, true),
									BootstrapTemplates.settingsInput(forms.Number.class, "Maximum RAM (MB)", "ramAmount", "Enter maximum amount of ram to use in MB", String.valueOf(minecraftServer.getColumnValue(MinecraftServerTable.MAX_HEAP_SIZE)), "RAM must be in 1024 increments", true, true)
										.addElements
										(
											new Small(String.format("This node has a total of %d MB of RAM. Memory Available: %d MB", totalRam, totalRam - reservedRam))
												.addClasses("form-text", "text-muted")
										),
									BootstrapTemplates.settingsInput(TextField.class, "Extra JVM Arguments", "arguments", "Enter optional arguments", minecraftServer.getColumnValue(MinecraftServerTable.ARGUMENTS), "These will be used when executing server start. Options -Xmx and -Xms are already set using the RAM given.", false, true),
									BootstrapTemplates.settingsInput(forms.Checkbox.class, "Automatic Restart", "restartsUnexpected", null, String.valueOf(minecraftServer.getColumnValue(MinecraftServerTable.AUTO_RESTARTS)), null, false, true),
									new LI()
										.addClasses("list-group-item")
										.addElements
										(
											new H1("Server Properties")
										)
								)
								.addElements
								(
									properties.keySet()
											  .stream()
											  .sorted()
											  .map(key -> BootstrapTemplates.settingsInput(minecraftPropertyToInput(MinecraftServer.MINECRAFT_PROPERTIES.get(key)),
													  	key, key, "Enter " + key, String.valueOf(properties.get(key)), null, false, true))
											  .toArray(CompoundElement[]::new)
								)
						)
				)
		);
		
		var heapInput = forms.Number.class.cast(content.getElementById("ramAmount"));
		heapInput.setMin(MinecraftServer.MINIMUM_HEAP_SIZE);
		heapInput.setStep(MinecraftServer.HEAP_STEP);
		
		var argumentInput = TextField.class.cast(content.getElementById("arguments"));
		argumentInput.removeClass("w-25");
		argumentInput.addClass("w-100");
		
		main.getBody().addScript("js/settings.js");
		
		setBody(main.getBody());
		setHead(main.getHead());
		
	}
	
	private static Class<? extends Input> minecraftPropertyToInput(Object prop)
	{
		if(prop instanceof Integer)
		{
			return forms.Number.class;
		}
		else if(prop instanceof Boolean)
		{
			return forms.Checkbox.class;
		}
		
		return forms.TextField.class;
	}
}
