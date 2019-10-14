package frontend;

import api.minecraft.MinecraftServer;
import backend.api.GameServerDelete;
import html.CompoundElement;
import tags.Anchor;
import tags.Button;
import tags.Script;
import util.Template;
import utils.JavaScriptUtils;
import utils.Utils;

public class Templates
{
	public static Template getMainTemplate()
	{
		Template t = new Template();
		
		CompoundElement wrapper = new CompoundElement("div");
		wrapper.setID("wrapper");
		wrapper.addClasses("d-flex", "justify-content-between");
		wrapper.addElement(getSideBar());
		
		CompoundElement content = new CompoundElement("div");
		content.addClasses("w-100", "flex-fill");
		content.setID("content");
		
		wrapper.addElement(content);
		
		t.getBody().addElement(wrapper);
		t.getHead().addStylesheet("css/bootstrap.min.css");
		t.getHead().addStylesheet("css/main.css");
		t.getBody().addScript("js/jquery-3.3.1.min.js");
		t.getBody().addScript("js/popper.min.js");
		t.getBody().addScript("js/bootstrap.js");
		t.getBody().addScript("js/all.js");
		return t;
	}
	
	private static CompoundElement getSideBar()
	{	
		CompoundElement sidebar = new CompoundElement("nav");
		sidebar.setID("sidebar");
		sidebar.addClasses("p-3", "flex-fill");
		
		Anchor homeLink = new Anchor();
		homeLink.setHref(Index.URL);
		homeLink.setID("homeLink");
		CompoundElement header = new CompoundElement("div");
		header.setID("sidebar-header");
		header.addElement(new CompoundElement("h2", "GameServer Hosting"));
		header.addClasses("text-center", "text-light", "overflow-hidden", "bg-secondary", "rounded");
		homeLink.addElement(header);
		
		CompoundElement list = new CompoundElement("div");
		list.addClasses("list-group", "list-group-flush", "mt-5", "text-center");
		
		Anchor serversItem = new Anchor();
		serversItem.setHref(Index.URL);
		serversItem.addClasses("list-group-item", "bg-secondary", "text-light");
		CompoundElement serversIcon = Templates.createIcon("server");
		CompoundElement serversText = new CompoundElement("span", "Servers");
		serversIcon.addClasses("mr-2");
		
		serversItem.addElement(serversIcon);
		serversItem.addElement(serversText);
		list.addElement(serversItem);
		
		Anchor nodesItem = new Anchor();
		nodesItem.setHref(NodesInfo.URL);
		nodesItem.addClasses("list-group-item", "bg-secondary", "text-light", "mt-3");
		CompoundElement nodesIcon = Templates.createIcon("project-diagram");
		CompoundElement nodesText = new CompoundElement("span", "Nodes");
		nodesIcon.addClasses("mr-2");
		
		nodesItem.addElement(nodesIcon);
		nodesItem.addElement(nodesText);		
		list.addElement(nodesItem);
		
		sidebar.addElement(homeLink);
		sidebar.addElement(list);
		
		return sidebar;
	}
	
	public static Anchor generateConsoleLink(String serverName)
	{
		Anchor consoleLink = new Anchor();
		consoleLink.addClasses("rounded-circle", "bg-light", "p-2");
		consoleLink.setHref(Utils.encodeURL(String.format("%s?name=%s", GameServerConsole.URL, serverName)));
		consoleLink.setAttribute("data-toggle", "tooltip");
		consoleLink.setAttribute("data-placement", "bottom");
		consoleLink.setTitle("Open server console");
		CompoundElement consoleIcon = Templates.createIcon("terminal");
		consoleLink.addElement(consoleIcon);
		return consoleLink;
	}
	
	public static Anchor generateFilesLink(String serverName)
	{
		Anchor filesLink = new Anchor();
		filesLink.addClasses("rounded-circle", "bg-light", "p-2");
		filesLink.setHref(Utils.encodeURL(String.format("%s?name=%s&directory=%s", GameServerFiles.URL, serverName, serverName)));
		filesLink.setAttribute("data-toggle", "tooltip");
		filesLink.setAttribute("data-placement", "bottom");
		filesLink.setTitle("View server files");
		CompoundElement filesIcon = Templates.createIcon("file");
		filesIcon.addClasses("fa-lg");
		filesLink.addElement(filesIcon);
		return filesLink;
	}
	
	public static Anchor generateSettingsLink(String serverName)
	{
		Anchor editLink = new Anchor();
		editLink.addClasses("rounded-circle", "bg-light", "p-2");
		editLink.setHref(Utils.encodeURL(String.format("%s?name=%s", GameServerSettings.URL, serverName)));
		editLink.setAttribute("data-toggle", "tooltip");
		editLink.setAttribute("data-placement", "bottom");
		editLink.setTitle("Edit server settings");
		CompoundElement editIcon = Templates.createIcon("edit");
		
		editLink.addElement(editIcon);
		return editLink;
	}
	
	public static Anchor generateDeleteLink(String serverName)
	{
		Anchor deleteLink = new Anchor();
		deleteLink.setHref("#");
		deleteLink.addClasses("rounded-circle", "bg-light", "p-2");
		deleteLink.setAttribute("data-toggle", "tooltip");
		deleteLink.setAttribute("data-placement", "bottom");
		deleteLink.setTitle("Delete server");
		deleteLink.setAttribute("link", Utils.encodeURL(String.format("%s?name=%s", GameServerDelete.URL, serverName)));
		deleteLink.setOnClick(String.format("deleteServer(this, '%s')", serverName));
		CompoundElement deleteIcon = Templates.createIcon("trash-alt");
		
		deleteLink.addElement(deleteIcon);
		return deleteLink;
	}
	
	/**
	 * Generates the canvas element that holds the graph.
	 * The page also needs to include moment.js, Chart.min.js,
	 * chartjs-plugin-streaming.js, nodeUsage.js, and the names
	 * of the nodes and addresses through the JavaScriptUtils class.
	 * @param nodeName
	 * @return
	 */
	public static CompoundElement generateNodeUsageGraph(String nodeName)
	{
		CompoundElement canvas = new CompoundElement("canvas");
		canvas.setID(nodeName + "-data");
		return canvas;
	}
	
	public static Script generateNodeNamesWithUsageAddresses()
	{
		return new Script(JavaScriptUtils.getNodeNames() + JavaScriptUtils.getNodeUsageAddresses());
	}
	
	public static CompoundElement createServerTableRow(String serverName, Class<?> type)
	{
		CompoundElement tableRow = new CompoundElement("tr");
		CompoundElement nameColumn = new CompoundElement("td", serverName);
		
		String typeName = "Unknown";
		
		if(type.equals(MinecraftServer.class))
		{
			typeName = "Minecraft";
		}
		
		CompoundElement typeColumn = new CompoundElement("td", typeName);
		
		CompoundElement statusIcon = Templates.createIcon("circle");
		statusIcon.addClasses("mr-2");
		
		CompoundElement statusColumn = new CompoundElement("td");
		statusColumn.setID("status-" + serverName);
		CompoundElement statusSpan = new CompoundElement("span", "Unknown");
		CompoundElement fixCircleColor = new CompoundElement("span");
		fixCircleColor.addElement(statusIcon);
		statusColumn.addElement(fixCircleColor);
		statusColumn.addElement(statusSpan);
		
		CompoundElement optionsColumn = new CompoundElement("td");
		optionsColumn.addClasses("pb-3");
		
		Anchor consoleLink = Templates.generateConsoleLink(serverName);
		consoleLink.addClass("mr-4");
		
		optionsColumn.addElement(consoleLink);
		
		Anchor filesLink = Templates.generateFilesLink(serverName);
		filesLink.addClass("mr-4");
		optionsColumn.addElement(filesLink);
		
		Anchor editLink = Templates.generateSettingsLink(serverName);
		editLink.addClass("mr-4");
		optionsColumn.addElement(editLink);
		
		Anchor deleteButton = Templates.generateDeleteLink(serverName);
		optionsColumn.addElement(deleteButton);
		
		CompoundElement startStopColumn = new CompoundElement("td");
		startStopColumn.addClass("float-right");
		
		Button start = generateStartButton();
		start.setOnClick(String.format("startServer('%s')", serverName));
		start.addClass("mr-4");
		start.setID(String.format("start-%s", serverName));
		start.setDisabled(true);
		Button stop = generateStopButton();
		stop.setOnClick(String.format("stopServer('%s')", serverName));
		stop.addClass("mr-4");
		stop.setDisabled(true);
		stop.setID(String.format("stop-%s", serverName));
		
		startStopColumn.addElement(start);
		startStopColumn.addElement(stop);
		
		tableRow.addElement(nameColumn);
		tableRow.addElement(typeColumn);
		tableRow.addElement(statusColumn);
		tableRow.addElement(optionsColumn);
		tableRow.addElement(startStopColumn);
		
		return tableRow;
	}
	
	public static Button generateStartButton()
	{
		Button start = new Button("Start Server");
		start.setID("start");
		start.addClasses("btn", "btn-primary");
		start.setOnClick("startServer()");
		return start;
	}
	
	public static Button generateStopButton()
	{
		Button stop = new Button("Stop Server");
		stop.setID("stop");
		stop.addClasses("btn", "btn-danger");
		stop.setOnClick("stopServer()");
		return stop;
	}
	
	public static CompoundElement createIcon(String fontAwesomeType)
	{
		CompoundElement icon = new CompoundElement("i");
		icon.addClasses("fas", String.format("fa-%s", fontAwesomeType));
		
		return icon;
	}
}
