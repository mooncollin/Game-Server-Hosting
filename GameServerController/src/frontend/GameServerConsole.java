package frontend;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.api.GameServerTriggerDelete;
import backend.api.GameServerTriggerEdit;
import backend.main.StartUpApplication;
import forms.Form;
import forms.TextField;
import html.CompoundElement;
import html.Element;
import model.Query;
import model.TableTemp;
import models.TriggersTable;
import server.GameServer;
import tags.Anchor;
import tags.Button;
import tags.Script;
import tags.TextArea;
import util.Template;
import utils.StringUtils;
import utils.Utils;

@WebServlet("/GameServerConsole")
public class GameServerConsole extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerConsole";

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
		
		if(serverName == null)
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
		
		Class<? extends GameServer> serverType = serverFound.getFirst();
		
		Template template = Templates.getMainTemplate();
		CompoundElement content = (CompoundElement) template.getBody().getElementById("content");
		if(serverType.equals(api.minecraft.MinecraftServer.class))
		{
			content.setStyle("background-image: url('" + Assets.getRandomMinecraftBackground() + "')");
		}
		template.getBody().addEndElement(new Script("var serverName='" + Utils.encodeURL(serverName) + "';"));
		template.getBody().addScript("js/server.js");
		
		CompoundElement leftElements = new CompoundElement("div");
		leftElements.addClasses("float-left", "ml-5");
		leftElements.setID("left-elements");
		
		CompoundElement title = new CompoundElement("h1", serverName);
		title.setID("title");
		title.addClass("text-light");
		
		TextArea textarea = new TextArea();
		textarea.setID("log");
		textarea.addClasses("form-control", "text-light", "bg-dark");
		textarea.setRows(20);
		textarea.setCols(50);
		textarea.setReadOnly(true);
		
		TextField commandText = new TextField();
		commandText.setID("command");
		commandText.addClasses("form-control");
		commandText.setAutoFocus(true);
		commandText.setOnKeyPress("command(event)");
		commandText.setOnKeyDown("history(event)");
		
		CompoundElement optionsList = new CompoundElement("ul");
		optionsList.setID("options");
		optionsList.addClasses("float-right", "list-group", "list-group-flush", "mr-3", "bg-dark", "text-center", "mb-5");
		Button startButton = Templates.generateStartButton();
		Button stopButton = Templates.generateStopButton();
		
		Anchor filesButton = new Anchor("View Files");
		filesButton.addClasses("btn", "btn-info");
		filesButton.setHref(Utils.encodeURL(String.format("%s?name=%s&directory=%s", GameServerFiles.URL, serverName, serverName)));
		
		Anchor settingsButton = new Anchor("View Settings");
		settingsButton.addClasses("btn", "btn-warning");
		settingsButton.setHref(Utils.encodeURL(String.format("%s?name=%s", GameServerSettings.URL, serverName)));
		
		CompoundElement startListItem = new CompoundElement("li");
		startListItem.addClasses("list-group-item", "bg-dark");
		startListItem.addElement(startButton);
		
		CompoundElement stopListItem = new CompoundElement("li");
		stopListItem.addClasses("list-group-item", "bg-dark");
		stopListItem.addElement(stopButton);
		
		CompoundElement filesListItem = new CompoundElement("li");
		filesListItem.addClasses("list-group-item", "bg-dark");
		filesListItem.addElement(filesButton);
		
		CompoundElement settingsListItem = new CompoundElement("li");
		settingsListItem.addClasses("list-group-item", "bg-dark");
		settingsListItem.addElement(settingsButton);
		
		optionsList.addElement(startListItem);
		optionsList.addElement(stopListItem);
		optionsList.addElement(filesListItem);
		optionsList.addElement(settingsListItem);
		
		leftElements.addElement(title);
		leftElements.addElement(new Element("hr"));
		leftElements.addElement(textarea);
		leftElements.addElement(commandText);
		
		CompoundElement triggerTabs = new CompoundElement("ul");
		triggerTabs.addClasses("nav", "nav-tabs", "mt-5", "bg-dark");
		
		CompoundElement allTriggers = new CompoundElement("li");
		allTriggers.addClass("nav-item");
		Anchor allTriggersLink = new Anchor("All Triggers");
		allTriggersLink.setHref("#allTriggers");
		allTriggersLink.addClasses("nav-link", "active");
		allTriggersLink.setAttribute("data-toggle", "tab");
		allTriggers.addElement(allTriggersLink);
		triggerTabs.addElement(allTriggers);
		
		CompoundElement timeTriggers = new CompoundElement("li");
		timeTriggers.addClass("nav-item");
		Anchor timeTriggersLink = new Anchor("Time Triggers");
		timeTriggersLink.setHref("#timeTriggers");
		timeTriggersLink.addClass("nav-link");
		timeTriggersLink.setAttribute("data-toggle", "tab");
		timeTriggers.addElement(timeTriggersLink);
		triggerTabs.addElement(timeTriggers);
		
		CompoundElement recurringTriggers = new CompoundElement("li");
		recurringTriggers.addClass("nav-item");
		Anchor recurringTriggersLink = new Anchor("Recurring Triggers");
		recurringTriggersLink.setHref("#recurringTriggers");
		recurringTriggersLink.addClass("nav-link");
		recurringTriggersLink.setAttribute("data-toggle", "tab");
		recurringTriggers.addElement(recurringTriggersLink);
		triggerTabs.addElement(recurringTriggers);
		
		CompoundElement outputTriggers = new CompoundElement("li");
		outputTriggers.addClass("nav-item");
		Anchor outputTriggersLink = new Anchor("Output Triggers");
		outputTriggersLink.setHref("#outputTriggers");
		outputTriggersLink.addClass("nav-link");
		outputTriggersLink.setAttribute("data-toggle", "tab");
		outputTriggers.addElement(outputTriggersLink);
		triggerTabs.addElement(outputTriggers);
		
		List<TableTemp> triggers;
		try
		{
			triggers = Query.query(StartUpApplication.database, TriggersTable.class)
								.filter(TriggersTable.SERVER_OWNER.cloneWithValue(serverName))
								.all();
		} catch (SQLException e)
		{
			response.setStatus(500);
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			return;
		}
		
		CompoundElement tabContents = new CompoundElement("div");
		tabContents.addClass("tab-content");
		tabContents.setID("tab-contents");
		tabContents.addElement(getTriggers(triggers, "allTriggers", serverName));
		tabContents.addElement(getTriggers(triggers, "timeTriggers", serverName));
		tabContents.addElement(getTriggers(triggers, "recurringTriggers", serverName));
		tabContents.addElement(getTriggers(triggers, "outputTriggers", serverName));
		
		leftElements.addElement(triggerTabs);
		leftElements.addElement(tabContents);
		
		content.addElement(leftElements);
		content.addElement(optionsList);
		response.setContentType("text/html");
		response.getWriter().print(template);
	}
	
	private CompoundElement getTriggers(List<TableTemp> triggers, String filterType, String serverName)
	{
		CompoundElement allTriggers = new CompoundElement("div");
		if(filterType.equals("allTriggers"))
		{
			allTriggers.addClasses("show", "active");
		}
		allTriggers.addClasses("tab-pane", "fade");
		allTriggers.setID(filterType);
		
		CompoundElement table = new CompoundElement("table");
		table.addClasses("table", "table-dark");
		table.setStyle("table-layout: fixed;");
		CompoundElement thead = new CompoundElement("thead");
		CompoundElement headingRow = new CompoundElement("tr");
		CompoundElement typeHeader = new CompoundElement("th", "Type");
		CompoundElement valueHeader = new CompoundElement("th", "Value");
		CompoundElement commandHeader = new CompoundElement("th", "Command");
		CompoundElement actionHeader = new CompoundElement("th", "Action");
		CompoundElement optionsHeader = new CompoundElement("th");
		
		Button addTrigger = new Button();
		addTrigger.addClasses("text-light", "btn", "btn-primary", "float-right");
		addTrigger.setAttribute("trigger", filterType);
		addTrigger.setOnClick("addTrigger(this)");
		CompoundElement addIcon = new CompoundElement("i");
		addIcon.addClasses("fas", "fa-plus");
		addTrigger.addElement(addIcon);
		optionsHeader.addElement(addTrigger);
		
		headingRow.addElement(typeHeader);
		headingRow.addElement(valueHeader);
		headingRow.addElement(commandHeader);
		headingRow.addElement(actionHeader);
		headingRow.addElement(optionsHeader);
		thead.addElement(headingRow);
		table.addElement(thead);
		
		CompoundElement tableBody = new CompoundElement("tbody");
		int index = 1;
		for(var trigger : triggers)
		{
			var triggerType 	= trigger.getColumnValue(TriggersTable.TYPE);
			var triggerValue 	= trigger.getColumnValue(TriggersTable.VALUE);
			var triggerID 		= trigger.getColumnValue(TriggersTable.ID);
			var triggerCommand 	= trigger.getColumnValue(TriggersTable.COMMAND);
			var triggerExtra	= trigger.getColumnValue(TriggersTable.EXTRA);
			if(filterType.equals("allTriggers") || filterType.contains(triggerType))
			{
				String formID = filterType + index;
				CompoundElement tableRow = new CompoundElement("tr");
				CompoundElement type = new CompoundElement("td", StringUtils.capitalize(triggerType));
				CompoundElement value = new CompoundElement("td");
				String valueResult = triggerValue;
				if(trigger.getColumnValue(TriggersTable.TYPE).equals("time"))
				{
					LocalTime time = LocalTime.MIDNIGHT.plusSeconds(Integer.valueOf(triggerValue));
					Calendar today = Calendar.getInstance();
					today.set(Calendar.HOUR_OF_DAY, time.getHour());
					today.set(Calendar.MINUTE, time.getMinute());
					today.set(Calendar.SECOND, time.getSecond());
					
					valueResult = time.toString();
				}
				else if(triggerType.equals("recurring"))
				{
					Duration dur = Duration.ofSeconds(Long.valueOf(triggerValue));
					valueResult = dur.toString().substring(2)
				            .replaceAll("(\\d[HMS])(?!$)", "$1 ")
				            .toLowerCase();
				}
				value.setData(valueResult);
				CompoundElement command = new CompoundElement("td", triggerCommand);
				CompoundElement action = new CompoundElement("td", Objects.requireNonNullElse(triggerExtra, ""));
				
				CompoundElement options = new CompoundElement("td");
				
				Button deleteButton = new Button();
				deleteButton.addClasses("btn-danger", "text-light", "btn", "float-right");
				deleteButton.setAttribute("link", Utils.encodeURL(String.format("%s?name=%s&id=%s", GameServerTriggerDelete.URL, serverName, triggerID)));
				deleteButton.setOnClick("deleteRow(this)");
				CompoundElement deleteIcon = new CompoundElement("i");
				deleteIcon.addClasses("fas", "fa-trash");
				deleteButton.addElement(deleteIcon);
				options.addElement(deleteButton);
				
				
				Button editButton = new Button();
				editButton.addClasses("btn-warning", "text-light", "btn", "float-right", "mr-2");
				editButton.setOnClick(String.format("editRow(this, %d, '%s')", index, filterType));
				CompoundElement editIcon = new CompoundElement("i");
				editIcon.addClasses("fas", "fa-edit");
				editButton.addElement(editIcon);
				options.addElement(editButton);
				
				tableRow.addElement(type);
				tableRow.addElement(value);
				tableRow.addElement(command);
				tableRow.addElement(action);
				tableRow.addElement(options);
				tableBody.addElement(tableRow);
				
				Form rowForm = new Form();
				rowForm.setMethod("POST");
				rowForm.setAction(Utils.encodeURL(String.format("%s?name=%s&id=%d", GameServerTriggerEdit.URL, serverName, triggerID)));
				rowForm.setID(formID);
				table.addEndElement(rowForm);
				index++;
			}
		}
		
		Form newForm = new Form();
		newForm.setMethod("POST");
		newForm.setAction(Utils.encodeURL(String.format("%s?name=%s&id=%d", GameServerTriggerEdit.URL, serverName, -1)));
		newForm.setID("newTrigger");
		table.addEndElement(newForm);
		
		table.addElement(tableBody);
		allTriggers.addElement(table);
		
		return allTriggers;
	}
}
