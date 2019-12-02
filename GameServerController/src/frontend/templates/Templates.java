package frontend.templates;

import java.util.stream.Stream;

import api.minecraft.MinecraftServer;
import attributes.Attributes;
import backend.api.GameServerDelete;
import frontend.GameServerConsole;
import frontend.GameServerFiles;
import frontend.GameServerSettings;
import frontend.Index;
import frontend.NodesInfo;
import frontend.javascript.JavaScriptUtils;
import html.CompoundElement;
import server.GameServer;
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

public class Templates
{
	public static final String[] GLOBAL_CSS_FILES =
	{
			"css/bootstrap.min.css",
			"css/main.css"
	};
	
	public static final String[] GLOBAL_JAVASCRIPT_FILES =
	{
			"js/jquery-3.3.1.min.js",
			"js/popper.min.js",
			"js/bootstrap.min.js",
			"js/all.js"
	};
	
	public static Template getMainTemplate()
	{
		var t = new Template();
		
		t.getBody().addElements
		(
			new Div(Attributes.ID.makeAttribute("wrapper"))
				.addClasses("d-flex", "justify-content-between")
				.addElements
				(
					getSideBar(),
					new Div(Attributes.ID.makeAttribute("content"))
						.addClasses("w-100", "flex-fill")
				)
		);
		
		Stream.of(GLOBAL_CSS_FILES)
			  .forEach(css -> t.getHead().addStylesheet(css));
		
		Stream.of(GLOBAL_JAVASCRIPT_FILES)
			  .forEach(javascript -> t.getHead().addScript(javascript));
		
		return t;
	}
	
	private static Nav getSideBar()
	{
		
		return new Nav(Attributes.ID.makeAttribute("sidebar"))
			.addClasses("p-3", "flex-fill")
			.addElements
			(
				new Anchor(Attributes.Href.makeAttribute(Index.getEndpoint().getURL()), Attributes.ID.makeAttribute("homeLink"))
					.addElements
					(
						new Div(Attributes.ID.makeAttribute("sidebar-header"))
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
						new Anchor(Attributes.Href.makeAttribute(Index.getEndpoint().getURL()))
							.addClasses("list-group-item", "bg-secondary", "text-light")
							.addElements
							(
								Templates.createIcon("server").addClasses("mr-2"),
								new Span("Servers")
							),
						new Anchor(Attributes.Href.makeAttribute(NodesInfo.getEndpoint().getURL()))
							.addClasses("list-group-item", "bg-secondary", "text-light", "mt-3")
							.addElements
							(
								Templates.createIcon("project-diagram").addClasses("mr-2"),
								new Span("Nodes")
							)
					)
			);
	}
	
	public static Anchor generateConsoleLink(int serverID)
	{
		return new Anchor
		(
			Attributes.Href.makeAttribute(GameServerConsole.getEndpoint(serverID).getURL()),
			Attributes.makeAttribute("data-toggle", "tooltip"),
			Attributes.makeAttribute("data-placement", "bottom"),
			Attributes.Title.makeAttribute("Open server console")
		)
		.addClasses("rounded-circle", "bg-light", "p-2")
		.addElements
		(
			Templates.createIcon("terminal")
		);
	}
	
	public static Anchor generateFilesLink(int serverID, String serverName)
	{
		return new Anchor
		(
			Attributes.Href.makeAttribute(GameServerFiles.getEndpoint(serverID, serverName).getURL()),
			Attributes.makeAttribute("data-toggle", "tooltip"),
			Attributes.makeAttribute("data-placement", "bottom"),
			Attributes.Title.makeAttribute("View server files")
		)
		.addClasses("rounded-circle", "bg-light", "p-2")
		.addElements
		(
			Templates.createIcon("file").addClasses("fa-lg")
		);
	}
	
	public static Anchor generateSettingsLink(int serverID)
	{
		return new Anchor
		(
			Attributes.Href.makeAttribute(GameServerSettings.getEndpoint(serverID).getURL()),
			Attributes.makeAttribute("data-toggle", "tooltip"),
			Attributes.makeAttribute("data-placement", "bottom"),
			Attributes.Title.makeAttribute("Edit server settings")
		)
		.addClasses("rounded-circle", "bg-light", "p-2")
		.addElements
		(
			Templates.createIcon("edit")
		);
	}
	
	public static Anchor generateDeleteLink(int serverID, String serverName)
	{
		return new Anchor
		(
			Attributes.Href.makeAttribute("#"),
			Attributes.makeAttribute("data-toggle", "tooltip"),
			Attributes.makeAttribute("data-placement", "bottom"),
			Attributes.Title.makeAttribute("Delete Server"),
			Attributes.makeAttribute("link", GameServerDelete.getEndpoint(serverID).getURL()),
			Attributes.OnClick.makeAttribute(String.format("deleteServer(this, '%s')", serverName))
		)
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
	
	@SuppressWarnings("unchecked")
	public static CompoundElement createServerTableRow(Integer serverID, String serverName, Class<? extends GameServer> type)
	{
		var typeName = "Unknown";
		
		if(type.equals(MinecraftServer.class))
		{
			typeName = "Minecraft";
		}
		
		return new TableRow()
		.addElements
		(
			new TableData(serverName),
			new TableData(typeName),
			new TableData(Attributes.ID.makeAttribute("status-" + serverID))
					.addElements
					(
						new Span()
							.addElements
							(
								Templates.createIcon("circle").addClasses("mr-2")
							),
						new Span("Unknown")
					),
			new TableData()
				.addClasses("pb-3")
				.addElements
				(
					Templates.generateConsoleLink(serverID).addClasses("mr-4"),
					Templates.generateFilesLink(serverID, serverName).addClasses("mr-4"),
					Templates.generateSettingsLink(serverID).addClasses("mr-4"),
					Templates.generateDeleteLink(serverID, serverName)
				),
			new TableData()
				.addClasses("float-right")
				.addElements
				(
					generateStartButton()
						.addAttributes
						(
							Attributes.ID.makeAttribute(String.format("start-%d", serverID)),
							Attributes.OnClick.makeAttribute(String.format("startServer(%d)", serverID)),
							Attributes.Disabled.makeAttribute(true)
						)
						.addClasses("mr-4"),
					generateStopButton()
						.addAttributes
						(
							Attributes.ID.makeAttribute(String.format("stop-%d", serverID)),
							Attributes.OnClick.makeAttribute(String.format("stopServer(%d)", serverID)),
							Attributes.Disabled.makeAttribute(true)
						)
						.addClasses("mr-4")
				)
		);
	}
	
	@SuppressWarnings("unchecked")
	public static Button generateStartButton()
	{
		return new Button("Start Server", Attributes.ID.makeAttribute("start"), Attributes.OnClick.makeAttribute("startServer()"))
				.addClasses("btn", "btn-primary")
				.addElements(BootstrapTemplates.makeSpinner(null, true)
								.addClasses("ml-2")	
								.addAttributes(Attributes.Hidden.makeAttribute(true)));
	}
	
	@SuppressWarnings("unchecked")
	public static Button generateStopButton()
	{
		return new Button("Stop Server", Attributes.ID.makeAttribute("stop"), Attributes.OnClick.makeAttribute("stopServer()"))
				.addClasses("btn", "btn-danger")
				.addElements(BootstrapTemplates.makeSpinner(null, true)
								.addClasses("ml-2")
								.addAttributes(Attributes.Hidden.makeAttribute(true)));
	}
	
	public static Icon createIcon(String fontAwesomeType)
	{
		return createIcon("fas", fontAwesomeType);
	}
	
	public static Icon createIcon(String preclass, String fontAwesomeType)
	{
		return new Icon().addClasses(preclass, "fa-" + fontAwesomeType);
	}
}
