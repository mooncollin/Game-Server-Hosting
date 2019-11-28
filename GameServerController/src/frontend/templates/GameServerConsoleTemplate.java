package frontend.templates;

import java.time.Duration;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import api.minecraft.MinecraftServer;
import attributes.Attributes;
import backend.api.GameServerTriggerDelete;
import backend.api.GameServerTriggerEdit;
import backend.api.ServerInteract;
import forms.Form;
import forms.TextField;
import frontend.Assets;
import frontend.GameServerFiles;
import frontend.GameServerSettings;
import frontend.javascript.JavascriptVariable;
import html.CompoundElement;
import html.Element;
import model.Table;
import models.TriggersTable;
import server.GameServer;
import tags.Anchor;
import tags.Button;
import tags.Div;
import tags.H1;
import tags.HR;
import tags.Icon;
import tags.LI;
import tags.Script;
import tags.TableBody;
import tags.TableData;
import tags.TableHead;
import tags.TableHeader;
import tags.TableRow;
import tags.TextArea;
import tags.UL;
import util.Template;
import utils.StringUtils;
import utils.Utils;

public class GameServerConsoleTemplate extends Template
{
	public GameServerConsoleTemplate(Class<? extends GameServer> serverType, int serverID, String serverName, List<Table> triggers)
	{
		Objects.requireNonNull(serverType);
		Objects.requireNonNull(serverName);
		
		var mainTemplate = Templates.getMainTemplate();
		var content = Div.class.cast(mainTemplate.getBody().getElementById("content"));
		if(serverType.equals(MinecraftServer.class))
		{
			content.getStyles().setBackgroundImage(String.format("url('%s')", Assets.getRandomMinecraftBackground()));
		}
		
		content.addElements
		(
			new Div(
				Map.ofEntries(
					Map.entry(Attributes.ID.ATTRIBUTE_NAME, "left-elements")
				))
				.addClasses("float-left", "ml-5")
				.addElements
				(
					new H1(serverName,
						Map.ofEntries(
							Map.entry(Attributes.ID.ATTRIBUTE_NAME, "title")
						))
						.addClasses("text-light"),
					new HR(),
					new TextArea(
						Map.ofEntries(
							Map.entry(Attributes.ID.ATTRIBUTE_NAME, "log"),
							Map.entry(Attributes.Rows.ATTRIBUTE_NAME, 20),
							Map.entry(Attributes.Cols.ATTRIBUTE_NAME, 50),
							Map.entry(Attributes.ReadOnly.ATTRIBUTE_NAME, true)
						))
						.addClasses("form-control", "text-light", "bg-dark"),
					new TextField(
						Map.ofEntries(
							Map.entry(Attributes.ID.ATTRIBUTE_NAME, "command"),
							Map.entry(Attributes.AutoFocus.ATTRIBUTE_NAME, true),
							Map.entry(Attributes.OnKeyDown.ATTRIBUTE_NAME, "history(event)"),
							Map.entry(Attributes.OnKeyPress.ATTRIBUTE_NAME, "command(event)")
						))
						.addClasses("form-control"),
					new UL()
						.addClasses("nav", "nav-tabs", "mt-5", "bg-dark")
						.addElements
						(
							new LI()
								.addClasses("nav-item")
								.addElements
								(
									new Anchor("All Triggers",
										Map.ofEntries(
											Map.entry(Attributes.Href.ATTRIBUTE_NAME, "#allTriggers"),
											Map.entry("data-toggle", "tab")
										))
										.addClasses("nav-link", "active")
								),
							new LI()
								.addClasses("nav-item")
								.addElements
								(
									new Anchor("Time Triggers",
										Map.ofEntries(
											Map.entry(Attributes.Href.ATTRIBUTE_NAME, "#timeTriggers"),
											Map.entry("data-toggle", "tab")
										))
										.addClasses("nav-link")
								),
							new LI()
							.addClasses("nav-item")
							.addElements
							(
								new Anchor("Recurring Triggers",
									Map.ofEntries(
										Map.entry(Attributes.Href.ATTRIBUTE_NAME, "#recurringTriggers"),
										Map.entry("data-toggle", "tab")
									))
									.addClasses("nav-link")
							),
							new LI()
							.addClasses("nav-item")
							.addElements
							(
								new Anchor("Output Triggers",
									Map.ofEntries(
										Map.entry(Attributes.Href.ATTRIBUTE_NAME, "#outputTriggers"),
										Map.entry("data-toggle", "tab")
									))
									.addClasses("nav-link")
							)
						),
						new Div(
							Map.ofEntries(
								Map.entry(Attributes.ID.ATTRIBUTE_NAME, "tab-contents")
							))
							.addClasses("tab-content")
							.addElements
							(
								getTriggers(triggers, "allTriggers", serverName),
								getTriggers(triggers, "timeTriggers", serverName),
								getTriggers(triggers, "recurringTriggers", serverName),
								getTriggers(triggers, "outputTriggers", serverName)
							)
				),
			new UL(
				Map.ofEntries(
					Map.entry(Attributes.ID.ATTRIBUTE_NAME, "options")
				))
				.addClasses("float-right", "list-group", "list-group-flush", "mr-3", "bg-dark", "text-center", "mb-5")
				.addElements
				(
					new LI()
						.addClasses("list-group-item", "bg-dark")
						.addElements
						(
							Templates.generateStartButton()
						),
					new LI()
						.addClasses("list-group-item", "bg-dark")
						.addElements
						(
							Templates.generateStopButton()
						),
					new LI()
						.addClasses("list-group-item", "bg-dark")
						.addElements
						(
							new Anchor("View Files",
								Map.ofEntries(
									Map.entry(Attributes.Href.ATTRIBUTE_NAME, GameServerFiles.getEndpoint(serverID, serverName))
								))
								.addClasses("btn", "btn-info")
						),
					new LI()
						.addClasses("list-group-item", "bg-dark")
						.addElements
						(
							new Anchor("View Settings",
								Map.ofEntries(
									Map.entry(Attributes.Href.ATTRIBUTE_NAME, GameServerSettings.getEndpoint(serverID))
								))
								.addClasses("btn", "btn-warning")
						)
				)
		);
		
		var serverIDVar = new JavascriptVariable<Integer>("serverID", serverID);
		var serverLocation = new JavascriptVariable<String>("serverLocation", ServerInteract.getEndpoint(serverID, null));
		
		mainTemplate.getBody().addEndElement(new Script(serverLocation.toString()));
		mainTemplate.getBody().addEndElement(new Script(serverIDVar.toString()));
		mainTemplate.getBody().addScript("js/server.js");
		setBody(mainTemplate.getBody());
		setHead(mainTemplate.getHead());
	}
	
	@SuppressWarnings("unchecked")
	private static CompoundElement getTriggers(List<Table> triggers, String filterType, String serverName)
	{
		var triggerIDs = new LinkedList<Integer>();
		
		return new Div()
		.addAttributes(Attributes.ID.makeAttribute(filterType))
		.addClasses(filterType.equals("allTriggers") ? new String[] {"show", "active"} : new String[] {})
		.addClasses("tab-pane", "fade")
		.addElements
		(
			new tags.Table()
				.addAttributes(Attributes.Style.makeAttribute("table-layout: fixed;"))
				.addClasses("table", "table-dark")
				.addElements
				(
					new TableHeader()
						.addElements
						(
							new TableRow()
								.addElements
								(
									new TableHead("Type"),
									new TableHead("Value"),
									new TableHead("Command"),
									new TableHead("Action"),
									new TableHead()
										.addElements
										(
											new Button()
												.addAttributes
												(
													Attributes.makeAttribute("trigger", filterType),
													Attributes.OnClick.makeAttribute("addTrigger(this)")
												)
												.addClasses("text-light", "btn", "btn-primary", "float-right")
												.addElements
												(
													new Icon().addClasses("fas", "fa-plus")
												)
										)
								)
						),
					new TableBody()
						.addElements
						(
							triggers.stream().filter(trigger -> 
								filterType.equals("allTriggers") ||
								filterType.contains(trigger.getColumnValue(TriggersTable.TYPE))
							).map(trigger -> {
								var triggerType 	= trigger.getColumnValue(TriggersTable.TYPE);
								var triggerValue 	= trigger.getColumnValue(TriggersTable.VALUE);
								var triggerID 		= trigger.getColumnValue(TriggersTable.ID);
								var triggerCommand 	= trigger.getColumnValue(TriggersTable.COMMAND);
								var triggerExtra	= trigger.getColumnValue(TriggersTable.EXTRA);
								
								var valueResult = triggerValue;
								
								if(trigger.getColumnValue(TriggersTable.TYPE).equals("time"))
								{
									var time = LocalTime.MIDNIGHT.plusSeconds(Integer.valueOf(triggerValue));
									valueResult = time.toString();
								}
								else if(triggerType.equals("recurring"))
								{
									var dur = Duration.ofSeconds(Long.valueOf(triggerValue));
									valueResult = dur.toString().substring(2)
								            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
								            .toLowerCase();
								}
								
								triggerIDs.add(triggerID);
								
								return new TableRow()
											.addElements
											(
												new TableData(StringUtils.capitalize(triggerType)),
												new TableData(valueResult),
												new TableData(triggerCommand),
												new TableData(Objects.requireNonNullElse(triggerExtra, "")),
												new TableData()
													.addElements
													(
														new Button()
															.addAttributes
															(
																Attributes.makeAttribute("link", Utils.encodeURL(String.format("%s?name=%s&id=%s", GameServerTriggerDelete.URL, serverName, triggerID))),
																Attributes.OnClick.makeAttribute("deleteRow(this)")
															)
															.addClasses("btn-danger", "text-light", "btn", "float-right")
															.addElements
															(
																new Icon().addClasses("fas", "fa-trash")
															),
														new Button()
															.addAttributes(Attributes.OnClick.makeAttribute(String.format("editRow(this, %d, '%s')", triggerID, filterType)))
															.addClasses("btn-warning", "text-light", "btn", "float-right", "mr-2")
															.addElements
															(
																new Icon().addClasses("fas", "fa-edit")
															)
													)
											);
							}).toArray(Element[]::new)
						)
				)
				.addElements
				(
					triggerIDs.stream().map(id -> {
						return new Form()
								.addAttributes
								(
									Attributes.Method.makeAttribute("POST"),
									Attributes.Action.makeAttribute(Utils.encodeURL(String.format("%s?name=%s&id=%d", GameServerTriggerEdit.URL, serverName, id))),
									Attributes.ID.makeAttribute(filterType + id)
								);
					}).toArray(Element[]::new)
				)
				.addElement
				(
					new Form()
						.addAttributes
						(
							Attributes.Method.makeAttribute("POST"),
							Attributes.Action.makeAttribute(Utils.encodeURL(String.format("%s?name=%s&id=%d", GameServerTriggerEdit.URL, serverName, -1))),
							Attributes.ID.makeAttribute("newTrigger")
						)
				)
		);
					
//		CompoundElement allTriggers = new CompoundElement("div");
//		if(filterType.equals("allTriggers"))
//		{
//			allTriggers.addClasses("show", "active");
//		}
//		allTriggers.addClasses("tab-pane", "fade");
//		allTriggers.setID(filterType);
//		
//		CompoundElement table = new CompoundElement("table");
//		table.addClasses("table", "table-dark");
//		table.setStyle("table-layout: fixed;");
//		CompoundElement thead = new CompoundElement("thead");
//		CompoundElement headingRow = new CompoundElement("tr");
//		CompoundElement typeHeader = new CompoundElement("th", "Type");
//		CompoundElement valueHeader = new CompoundElement("th", "Value");
//		CompoundElement commandHeader = new CompoundElement("th", "Command");
//		CompoundElement actionHeader = new CompoundElement("th", "Action");
//		CompoundElement optionsHeader = new CompoundElement("th");
//		
//		Button addTrigger = new Button();
//		addTrigger.addClasses("text-light", "btn", "btn-primary", "float-right");
//		addTrigger.setAttribute("trigger", filterType);
//		addTrigger.setOnClick("addTrigger(this)");
//		CompoundElement addIcon = new CompoundElement("i");
//		addIcon.addClasses("fas", "fa-plus");
//		addTrigger.addElement(addIcon);
//		optionsHeader.addElement(addTrigger);
//		
//		headingRow.addElement(typeHeader);
//		headingRow.addElement(valueHeader);
//		headingRow.addElement(commandHeader);
//		headingRow.addElement(actionHeader);
//		headingRow.addElement(optionsHeader);
//		thead.addElement(headingRow);
//		table.addElement(thead);
//		
//		CompoundElement tableBody = new CompoundElement("tbody");
//		int index = 1;
//		for(var trigger : triggers)
//		{
//			var triggerType 	= trigger.getColumnValue(TriggersTable.TYPE);
//			var triggerValue 	= trigger.getColumnValue(TriggersTable.VALUE);
//			var triggerID 		= trigger.getColumnValue(TriggersTable.ID);
//			var triggerCommand 	= trigger.getColumnValue(TriggersTable.COMMAND);
//			var triggerExtra	= trigger.getColumnValue(TriggersTable.EXTRA);
//			if(filterType.equals("allTriggers") || filterType.contains(triggerType))
//			{
//				String formID = filterType + index;
//				CompoundElement tableRow = new CompoundElement("tr");
//				CompoundElement type = new CompoundElement("td", StringUtils.capitalize(triggerType));
//				CompoundElement value = new CompoundElement("td");
//				String valueResult = triggerValue;
//				if(trigger.getColumnValue(TriggersTable.TYPE).equals("time"))
//				{
//					LocalTime time = LocalTime.MIDNIGHT.plusSeconds(Integer.valueOf(triggerValue));
//					valueResult = time.toString();
//				}
//				else if(triggerType.equals("recurring"))
//				{
//					Duration dur = Duration.ofSeconds(Long.valueOf(triggerValue));
//					valueResult = dur.toString().substring(2)
//				            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
//				            .toLowerCase();
//				}
//				value.setData(valueResult);
//				CompoundElement command = new CompoundElement("td", triggerCommand);
//				CompoundElement action = new CompoundElement("td", Objects.requireNonNullElse(triggerExtra, ""));
//				
//				CompoundElement options = new CompoundElement("td");
//				
//				Button deleteButton = new Button();
//				deleteButton.addClasses("btn-danger", "text-light", "btn", "float-right");
//				deleteButton.setAttribute("link", Utils.encodeURL(String.format("%s?name=%s&id=%s", GameServerTriggerDelete.URL, serverName, triggerID)));
//				deleteButton.setOnClick("deleteRow(this)");
//				CompoundElement deleteIcon = new CompoundElement("i");
//				deleteIcon.addClasses("fas", "fa-trash");
//				deleteButton.addElement(deleteIcon);
//				options.addElement(deleteButton);
//				
//				
//				Button editButton = new Button();
//				editButton.addClasses("btn-warning", "text-light", "btn", "float-right", "mr-2");
//				editButton.setOnClick(String.format("editRow(this, %d, '%s')", index, filterType));
//				CompoundElement editIcon = new CompoundElement("i");
//				editIcon.addClasses("fas", "fa-edit");
//				editButton.addElement(editIcon);
//				options.addElement(editButton);
//				
//				tableRow.addElement(type);
//				tableRow.addElement(value);
//				tableRow.addElement(command);
//				tableRow.addElement(action);
//				tableRow.addElement(options);
//				tableBody.addElement(tableRow);
//				
//				Form rowForm = new Form();
//				rowForm.setMethod("POST");
//				rowForm.setAction(Utils.encodeURL(String.format("%s?name=%s&id=%d", GameServerTriggerEdit.URL, serverName, triggerID)));
//				rowForm.setID(formID);
//				table.addEndElement(rowForm);
//				index++;
//			}
//		}
//		
//		Form newForm = new Form();
//		newForm.setMethod("POST");
//		newForm.setAction(Utils.encodeURL(String.format("%s?name=%s&id=%d", GameServerTriggerEdit.URL, serverName, -1)));
//		newForm.setID("newTrigger");
//		table.addEndElement(newForm);
//		
//		table.addElement(tableBody);
//		allTriggers.addElement(table);
//		
//		return allTriggers;
	}
}
