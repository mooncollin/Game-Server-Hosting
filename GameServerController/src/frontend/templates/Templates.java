package frontend.templates;

import java.util.Map;
import java.util.stream.Stream;

import api.minecraft.MinecraftServer;
import attributes.Attributes;
import backend.api.GameServerDelete;
import frontend.GameServerConsole;
import frontend.GameServerFiles;
import frontend.GameServerSettings;
import frontend.Index;
import frontend.NodesInfo;
import html.CompoundElement;
import tags.Anchor;
import tags.Button;
import tags.Canvas;
import tags.Div;
import tags.H2;
import tags.Icon;
import tags.Nav;
import tags.Script;
import tags.Span;
import tags.TableData;
import tags.TableRow;
import util.Template;
import utils.JavaScriptUtils;
import utils.Utils;

public class Templates
{
	public static final String[] CSS_FILES =
	{
			"css/bootstrap.min.css",
			"css/main.css"
	};
	
	public static final String[] JAVASCRIPT_FILES =
	{
			"js/jquery-3.3.1.min.js",
			"js/popper.min.js",
			"js/bootstrap.js",
			"js/all.js"
	};
	
	public static Template getMainTemplate()
	{
		var t = new Template();
		
		t.getBody().addElements
		(
			new Div(
				Map.ofEntries(
					Map.entry(Attributes.ID.ATTRIBUTE_NAME, "wrapper")
				))
				.addClasses("d-flex", "justify-content-between")
				.addElements
				(
					getSideBar(),
					new Div(
						Map.ofEntries(
							Map.entry(Attributes.ID.ATTRIBUTE_NAME, "content")
						))
						.addClasses("w-100", "flex-fill")
				)
		);
		
		Stream.of(CSS_FILES)
			  .forEach(css -> t.getHead().addStylesheet(css));
		
		Stream.of(JAVASCRIPT_FILES)
			  .forEach(javascript -> t.getHead().addScript(javascript));
		
		return t;
	}
	
	private static Nav getSideBar()
	{
		
		return new Nav
		(
			Map.ofEntries(
				Map.entry(Attributes.ID.ATTRIBUTE_NAME, "sidebar")
			))
			.addClasses("p-3", "flex-fill")
			.addElements
			(
				new Anchor(
					Map.ofEntries(
						Map.entry(Attributes.Href.ATTRIBUTE_NAME, Index.URL),
						Map.entry(Attributes.ID.ATTRIBUTE_NAME, "homeLink")
					))
					.addElements
					(
						new Div(
							Map.ofEntries(
								Map.entry(Attributes.ID.ATTRIBUTE_NAME, "sidebar-header")
							))
							.addClasses("text-center", "text-light", "overflow-hidden", "bg-secondary", "rounded")
							.addElements
							(
								new H2("GameServer Hosting")
							)
					),
				new Div()
					.addClasses("list-group", "list-group-flush", "mt-5", "text-center")
					.addElements
					(
						new Anchor(
							Map.ofEntries(
								Map.entry(Attributes.Href.ATTRIBUTE_NAME, Index.URL)
							))
							.addClasses("list-group-item", "bg-secondary", "text-light")
							.addElements
							(
								Templates.createIcon("server").addClasses("mr-2"),
								new Span("Servers")
							),
						new Anchor(
							Map.ofEntries(
								Map.entry(Attributes.Href.ATTRIBUTE_NAME, NodesInfo.URL)
							))
							.addClasses("list-group-item", "bg-secondary", "text-light", "mt-3")
							.addElements
							(
								Templates.createIcon("project-diagram").addClasses("mr-2"),
								new Span("Nodes")
							)
					)
			);
	}
	
	public static Anchor generateConsoleLink(String serverName)
	{
		return new Anchor
		(
			Map.ofEntries(
				Map.entry(Attributes.Href.ATTRIBUTE_NAME, Utils.encodeURL(String.format("%s?name=%s", GameServerConsole.URL, serverName))),
				Map.entry("data-toggle", "tooltip"),
				Map.entry("data-placement", "bottom"),
				Map.entry(Attributes.Title.ATTRIBUTE_NAME, "Open server console")
		))
		.addClasses("rounded-circle", "bg-light", "p-2")
		.addElements
		(
			Templates.createIcon("terminal")
		);
	}
	
	public static Anchor generateFilesLink(String serverName)
	{
		return new Anchor
		(
			Map.ofEntries(
				Map.entry(Attributes.Href.ATTRIBUTE_NAME, Utils.encodeURL(String.format("%s?name=%s&directory=%s", GameServerFiles.URL, serverName, serverName))),
				Map.entry("data-toggle", "tooltip"),
				Map.entry("data-placement", "bottom"),
				Map.entry(Attributes.Title.ATTRIBUTE_NAME, "View server files")
		))
		.addClasses("rounded-circle", "bg-light", "p-2")
		.addElements
		(
			Templates.createIcon("file").addClasses("fa-lg")
		);
	}
	
	public static Anchor generateSettingsLink(String serverName)
	{
		return new Anchor
		(
			Map.ofEntries(
				Map.entry(Attributes.Href.ATTRIBUTE_NAME, Utils.encodeURL(String.format("%s?name=%s", GameServerSettings.URL, serverName))),
				Map.entry("data-toggle", "tooltip"),
				Map.entry("data-placement", "bottom"),
				Map.entry(Attributes.Title.ATTRIBUTE_NAME, "Edit server settings")
		))
		.addClasses("rounded-circle", "bg-light", "p-2")
		.addElements
		(
			Templates.createIcon("edit")
		);
	}
	
	public static Anchor generateDeleteLink(String serverName)
	{
		return new Anchor
		(
			Map.ofEntries(
				Map.entry(Attributes.Href.ATTRIBUTE_NAME, "#"),
				Map.entry("data-toggle", "tooltip"),
				Map.entry("data-placement", "bottom"),
				Map.entry(Attributes.Title.ATTRIBUTE_NAME, "Delete server"),
				Map.entry("link", Utils.encodeURL(String.format("%s?name=%s", GameServerDelete.URL, serverName))),
				Map.entry(Attributes.OnClick.ATTRIBUTE_NAME, String.format("deleteServer(this, '%s')", serverName))
		))
		.addClasses("rounded-circle", "bg-light", "p-2")
		.addElements
		(
			Templates.createIcon("trash-alt")
		);
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
		var canvas = new Canvas();
		canvas.setID(nodeName + "-data");
		return canvas;
	}
	
	public static Script generateNodeNamesWithUsageAddresses()
	{
		return new Script(JavaScriptUtils.getNodeNames() + JavaScriptUtils.getNodeUsageAddresses());
	}
	
	public static CompoundElement createServerTableRow(String serverName, Class<?> type)
	{
		var tableRow = new TableRow();
		var nameColumn = new TableData(serverName);
		
		var typeName = "Unknown";
		
		if(type.equals(MinecraftServer.class))
		{
			typeName = "Minecraft";
		}
		
		var typeColumn = new TableData(typeName);
		
		var statusIcon = Templates.createIcon("circle");
		statusIcon.addClasses("mr-2");
		
		var statusColumn = new TableData();
		statusColumn.setID("status-" + serverName);
		var statusSpan = new Span("Unknown");
		var fixCircleColor = new Span();
		fixCircleColor.addElement(statusIcon);
		statusColumn.addElement(fixCircleColor);
		statusColumn.addElement(statusSpan);
		
		var optionsColumn = new TableData();
		optionsColumn.addClasses("pb-3");
		
		var consoleLink = Templates.generateConsoleLink(serverName);
		consoleLink.addClass("mr-4");
		
		optionsColumn.addElement(consoleLink);
		
		var filesLink = Templates.generateFilesLink(serverName);
		filesLink.addClass("mr-4");
		optionsColumn.addElement(filesLink);
		
		var editLink = Templates.generateSettingsLink(serverName);
		editLink.addClass("mr-4");
		optionsColumn.addElement(editLink);
		
		var deleteButton = Templates.generateDeleteLink(serverName);
		optionsColumn.addElement(deleteButton);
	
		var startStopColumn = new TableData();
		startStopColumn.addClass("float-right");
		
		var start = generateStartButton();
		start.setOnClick(String.format("startServer('%s')", serverName));
		start.addClass("mr-4");
		start.setID(String.format("start-%s", serverName));
		start.setDisabled(true);
		var stop = generateStopButton();
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
		var start = new Button("Start Server");
		start.setID("start");
		start.addClasses("btn", "btn-primary");
		start.setOnClick("startServer()");
		return start;
	}
	
	public static Button generateStopButton()
	{
		var stop = new Button("Stop Server");
		stop.setID("stop");
		stop.addClasses("btn", "btn-danger");
		stop.setOnClick("stopServer()");
		return stop;
	}
	
	public static Icon createIcon(String fontAwesomeType)
	{	
		return new Icon().addClasses("fas", String.format("fa-%s", fontAwesomeType));
	}
}
