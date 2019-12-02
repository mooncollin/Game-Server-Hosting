package frontend.templates;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import api.minecraft.MinecraftServer;
import api.minecraft.MinecraftServerCommandHandler;
import attributes.Attributes;
import backend.api.GameServerTriggerDelete;
import backend.api.GameServerTriggerEdit;
import forms.Form;
import forms.TextField;
import frontend.Assets;
import frontend.GameServerFiles;
import frontend.GameServerSettings;
import frontend.javascript.JavaScriptUtils;
import frontend.javascript.JavascriptMap;
import frontend.javascript.JavascriptVariable;
import html.CompoundElement;
import html.Element;
import model.Table;
import models.TriggersTable;
import server.GameServer;
import server.GameServerCommandHandler;
import server.TriggerHandler;
import server.TriggerHandlerRecurring;
import server.TriggerHandlerTime;
import tags.Anchor;
import tags.Button;
import tags.Div;
import tags.H1;
import tags.HR;
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
	public GameServerConsoleTemplate(String serverAddress, Class<? extends GameServer> serverType, int serverID, String serverName, List<Table> triggers)
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
			new Div(Attributes.ID.makeAttribute("left-elements"))
				.addClasses("float-left", "ml-5")
				.addElements
				(
					new H1(serverName, Attributes.ID.makeAttribute("title"))
						.addClasses("text-light"),
					new HR(),
					new TextArea
						(
							Attributes.ID.makeAttribute("log"),
							Attributes.Rows.makeAttribute(20),
							Attributes.Cols.makeAttribute(50),
							Attributes.ReadOnly.makeAttribute(true),
							Attributes.Value.makeAttribute("")
						)
						.addClasses("form-control", "text-light", "bg-dark"),
					new TextField
						(
							Attributes.ID.makeAttribute("command"),
							Attributes.AutoFocus.makeAttribute(true),
							Attributes.OnKeyDown.makeAttribute("history(event)"),
							Attributes.OnKeyPress.makeAttribute("command(event)")
						)
						.addClasses("form-control"),
					new UL()
						.addClasses("nav", "nav-tabs", "mt-5", "bg-dark")
						.addElements
						(
							new LI()
								.addClasses("nav-item")
								.addElements
								(
									new Anchor("All Triggers", Attributes.Href.makeAttribute("#allTriggers"), Attributes.makeAttribute("data-toggle", "tab"))
										.addClasses("nav-link", "active")
								),
							new LI()
								.addClasses("nav-item")
								.addElements
								(
									new Anchor("Time Triggers", Attributes.Href.makeAttribute("#timeTriggers"), Attributes.makeAttribute("data-toggle", "tab"))
										.addClasses("nav-link")
								),
							new LI()
							.addClasses("nav-item")
							.addElements
							(
								new Anchor("Recurring Triggers", Attributes.Href.makeAttribute("#recurringTriggers"), Attributes.makeAttribute("data-toggle", "tab"))
									.addClasses("nav-link")
							),
							new LI()
							.addClasses("nav-item")
							.addElements
							(
								new Anchor("Output Triggers", Attributes.Href.makeAttribute("#outputTriggers"), Attributes.makeAttribute("data-toggle", "tab"))
									.addClasses("nav-link")
							)
						),
						new Div(Attributes.ID.makeAttribute("tab-contents"))
							.addClasses("tab-content")
							.addElements
							(
								getTriggers(serverID, triggers, "allTriggers"),
								getTriggers(serverID, triggers, "timeTriggers"),
								getTriggers(serverID, triggers, "recurringTriggers"),
								getTriggers(serverID, triggers, "outputTriggers")
							)
				),
			new UL(Attributes.ID.makeAttribute("options"))
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
							new Anchor("View Files", Attributes.Href.makeAttribute(GameServerFiles.getEndpoint(serverID, serverName).getURL()))
								.addClasses("btn", "btn-info")
						),
					new LI()
						.addClasses("list-group-item", "bg-dark")
						.addElements
						(
							new Anchor("View Settings", Attributes.Href.makeAttribute(GameServerSettings.getEndpoint(serverID).getURL()))
								.addClasses("btn", "btn-warning")
						)
				)
		);
		
		
		var triggerOldValues = new JavascriptMap<Integer, JavascriptMap<String, String>>("triggerOldValues");
		
		for(var trigger : triggers)
		{
			var triggerID = trigger.getColumnValue(TriggersTable.ID);
			var triggerValue = trigger.getColumnValue(TriggersTable.VALUE);
			var triggerCommand = trigger.getColumnValue(TriggersTable.COMMAND);
			var triggerAction = trigger.getColumnValue(TriggersTable.EXTRA);
			var triggerType = trigger.getColumnValue(TriggersTable.TYPE);
			
			if(triggerType.equals(TriggerHandler.RECURRING_TYPE))
			{
				var seconds = Utils.fromString(Long.class, triggerValue);
				triggerValue = TriggerHandlerRecurring.convertSecondsToFormat(seconds);
			}
			else if(triggerType.equals(TriggerHandler.TIME_TYPE))
			{
				var seconds = Utils.fromString(Long.class, triggerValue);
				triggerValue = TriggerHandlerTime.convertSecondsToFormat(seconds);
			}
			
			var triggerMap = new JavascriptMap<String, String>("");
			triggerMap.set("value", triggerValue);
			triggerMap.set("command", triggerCommand);
			triggerMap.set("action", triggerAction);
			
			triggerOldValues.set(triggerID, triggerMap);
		}
		
		var serverIDVar = new JavascriptVariable<Integer>("serverID", serverID);
		var socketAddress = JavaScriptUtils.createSocketAddress("socketAddress", serverAddress, serverID);
		var serverCommandRequest = JavaScriptUtils.createInteractAddress("serverCommandRequest", serverID, MinecraftServerCommandHandler.SERVER_COMMAND_COMMAND);
		var serverStartRequest = JavaScriptUtils.createInteractAddress("serverStartRequest", serverID, GameServerCommandHandler.START_COMMAND);
		var serverStopRequest = JavaScriptUtils.createInteractAddress("serverStopRequest", serverID, GameServerCommandHandler.STOP_COMMAND);
		var serverLastRequest = JavaScriptUtils.createInteractAddress("serverLastRequest", serverID, MinecraftServerCommandHandler.LAST_COMMAND);
		var serverCommandEnd = new JavascriptVariable<String>("serverCommandEnd", "&" + MinecraftServerCommandHandler.SERVER_COMMAND_COMMAND + "=");
		var triggerTypes = JavaScriptUtils.triggerTypes("triggerTypes");
		var actionTypes = JavaScriptUtils.actionTypes("actionTypes");
		
		mainTemplate.getBody().addEndElement(new Script(serverIDVar.toString()));
		mainTemplate.getBody().addEndElement(new Script(socketAddress.toString()));
		mainTemplate.getBody().addEndElement(new Script(serverStartRequest.toString()));
		mainTemplate.getBody().addEndElement(new Script(serverStopRequest.toString()));
		mainTemplate.getBody().addEndElement(new Script(serverLastRequest.toString()));
		mainTemplate.getBody().addEndElement(new Script(serverCommandRequest.toString()));
		mainTemplate.getBody().addEndElement(new Script(triggerTypes.toString()));
		mainTemplate.getBody().addEndElement(new Script(actionTypes.toString()));
		mainTemplate.getBody().addEndElement(new Script(triggerOldValues.toString()));
		mainTemplate.getBody().addEndElement(new Script(serverCommandEnd.toString()));
		mainTemplate.getBody().addScript("js/server.js");
		setBody(mainTemplate.getBody());
		setHead(mainTemplate.getHead());
	}
	
	private static CompoundElement getTriggers(int serverID, List<Table> triggers, String filterType)
	{
		var triggerIDs = new LinkedList<Integer>();
		
		return new Div(Attributes.ID.makeAttribute(filterType))
		.addClasses(filterType.equals("allTriggers") ? new String[] {"show", "active"} : null)
		.addClasses("tab-pane", "fade")
		.addElements
		(
			new tags.Table(Attributes.Style.makeAttribute("table-layout: fixed;"))
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
											new Button
												(
													Attributes.makeAttribute("trigger", filterType),
													Attributes.OnClick.makeAttribute("addTrigger(this)")
												)
												.addClasses("text-light", "btn", "btn-primary", "float-right")
												.addElements
												(
													Templates.createIcon("plus")
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
								
								if(trigger.getColumnValue(TriggersTable.TYPE).equals(TriggerHandler.TIME_TYPE))
								{
									var seconds = Utils.fromString(Long.class, triggerValue);
									valueResult = TriggerHandlerTime.convertSecondsToFormat(seconds);
								}
								else if(triggerType.equals("recurring"))
								{
									var seconds = Utils.fromString(Long.class, triggerValue);
									valueResult = TriggerHandlerRecurring.convertSecondsToFormat(seconds);
								}
								
								triggerIDs.add(triggerID);
								
								return new TableRow(Attributes.ID.makeAttribute(String.valueOf(triggerID)))
											.addElements
											(
												new TableData(StringUtils.capitalize(triggerType)),
												new TableData(valueResult),
												new TableData(triggerCommand),
												new TableData(Objects.requireNonNullElse(triggerExtra, "")),
												new TableData()
													.addElements
													(
														new Button
															(
																Attributes.makeAttribute("link", GameServerTriggerDelete.getEndpoint(serverID, triggerID)),
																Attributes.OnClick.makeAttribute("deleteRow(this)")
															)
															.addClasses("btn-danger", "text-light", "btn", "float-right")
															.addElements
															(
																Templates.createIcon("trash")
															),
														new Button(Attributes.OnClick.makeAttribute(String.format("editRow(this, %d, '%s')", triggerID, filterType)))
															.addClasses("btn-warning", "text-light", "btn", "float-right", "mr-2")
															.addElements
															(
																Templates.createIcon("edit")
															)
													)
											);
							}).toArray(Element[]::new)
						)
				)
				.addElements
				(
					triggerIDs.stream().map(id -> {
						return new Form
						(
							Attributes.Method.makeAttribute("POST"),
							Attributes.Action.makeAttribute(GameServerTriggerEdit.postEndpoint(serverID, id).getURL()),
							Attributes.ID.makeAttribute(filterType + id)
						);
					}).toArray(Element[]::new)
				)
				.addElement
				(
					new Form
						(
							Attributes.Method.makeAttribute("POST"),
							Attributes.Action.makeAttribute(GameServerTriggerEdit.postEndpoint(serverID, -1).getURL()),
							Attributes.ID.makeAttribute("newTrigger")
						)
				)
		);
	}
}
