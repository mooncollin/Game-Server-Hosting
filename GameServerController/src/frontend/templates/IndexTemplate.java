package frontend.templates;

import java.util.Map;
import java.util.Set;

import attributes.Attributes;
import frontend.GameTypeAdd;
import html.Element;
import server.GameServer;
import tags.Anchor;
import tags.Div;
import tags.H1;
import tags.HR;
import tags.Script;
import tags.Table;
import tags.TableBody;
import tags.TableHead;
import tags.TableHeader;
import tags.TableRow;
import util.Template;
import utils.Pair;
import utils.Utils;

public class IndexTemplate extends Template
{
	public IndexTemplate(Set<Map.Entry<String, Pair<Class<? extends GameServer>, String>>> servers)
	{
		var main = Templates.getMainTemplate();
		var content = Div.class.cast(main.getBody().getElementById("content"));
		content.getStyles().setBackgroundImage("url('images/material-back.jpeg')");
		
		content.addElements
		(
			new Div
			(
				Map.ofEntries(Map.entry(Attributes.ID.ATTRIBUTE_NAME, "inner-content")))
			   .addClasses("ml-5", "mt-5")
			   .addElements
			   (
					new H1("Current Servers").addClasses("text-light").addElements
					(
						new Anchor(
							Map.ofEntries(
								Map.entry(Attributes.Href.ATTRIBUTE_NAME, GameTypeAdd.URL),
								Map.entry("data-toggle", "tooltip"),
								Map.entry("data-placement", "bottom"),
								Map.entry(Attributes.Title.ATTRIBUTE_NAME, "Add game type")))
							.addClasses("rounded-circle", "bg-light", "p-3", "float-right", "addLink")
							.addElements
							(
								Templates.createIcon("plus").addClasses("mr-2"),
								Templates.createIcon("gamepad")
							),
						new Anchor(
							Map.ofEntries(
								Map.entry(Attributes.Href.ATTRIBUTE_NAME, frontend.GameServerAdd.URL),
								Map.entry("data-toggle", "tooltip"),
								Map.entry("data-placement", "bottom"),
								Map.entry(Attributes.Title.ATTRIBUTE_NAME, "Add game server")))
							.addClasses("rounded-circle", "bg-light", "p-3", "float-right", "addLink", "mr-3")
							.addElements
							(
								Templates.createIcon("plus").addClasses("mr-2"),
								Templates.createIcon("server")
							)
					),
					new HR().addClasses("border", "border-light"),
					new Table().addClasses("table", "table-striped", "table-hover", "table-dark")
					.addElements
					(
						new TableHeader().addElements
						(
							new TableRow().addElements
							(
								new TableHead("Name",
									Map.ofEntries(
										Map.entry("scope", "col")
									)
								),
								new TableHead("Type",
									Map.ofEntries(
										Map.entry("scope", "col")
									)
								),
								new TableHead("Status",
									Map.ofEntries(
										Map.entry("scope", "col")
									)
								),
								new TableHead("Options",
									Map.ofEntries(
										Map.entry("scope", "col")
									)
								)
							)
						),
						new TableBody().addElements
						(
							servers.stream().map((serverEntry) -> {
								var serverName = serverEntry.getKey();
								var serverType = serverEntry.getValue().getFirst();
								
								return Templates.createServerTableRow(serverName, serverType);
							}).toArray(Element[]::new)
						)
					)
				)
	    );
		
		var nodeAddresses = "var nodeAddresses=[";
		var serverNames = "var serverNames=[";
		
		for(var serverEntry : servers)
		{
			var nodeAddress = serverEntry.getValue().getSecond();
			var serverName = serverEntry.getKey();
			
			nodeAddresses += String.format("'ws://%s/Output?name=%s&mode=running',", nodeAddress, Utils.encodeURL(serverName));
			serverNames += String.format("'%s',", serverName);
		}
		
		nodeAddresses += "];";
		serverNames += "];";
		
		main.getBody().addEndElement(new Script(nodeAddresses + serverNames));
		main.getBody().addScript("js/index.js");
		
		setBody(main.getBody());
		setHead(main.getHead());
	}
}
