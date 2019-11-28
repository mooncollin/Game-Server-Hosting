package frontend.templates;

import java.util.Map;
import attributes.Attributes;
import backend.main.StartUpApplication;
import frontend.GameTypeAdd;
import frontend.javascript.JavaScriptUtils;
import html.Element;
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

public class IndexTemplate extends Template
{
	public IndexTemplate(Map<Integer, String> serverNames)
	{
		var main = Templates.getMainTemplate();
		var content = Div.class.cast(main.getBody().getElementById("content"));
		content.getStyles().setBackgroundImage("url('images/material-back.jpeg')");
		
		content.addElements
		(
			new Div
			(
				Attributes.ID.makeAttribute("inner-content")
			)
		   .addClasses("ml-5", "mt-5")
		   .addElements
		   (
				new H1("Current Servers").addClasses("text-light").addElements
				(
					new Anchor
					(
						Attributes.Href.makeAttribute(GameTypeAdd.URL),
						Attributes.Title.makeAttribute("Add game type"),
						Attributes.makeAttribute("data-toggle", "tooltip"),
						Attributes.makeAttribute("data-placement", "bottom")
					)
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
							StartUpApplication.serverTypes.entrySet().stream().map(entry -> {
								var serverID = entry.getKey();
								var serverType = entry.getValue();
								var serverName = serverNames.get(serverID);
								
								return Templates.createServerTableRow(serverID, serverName, serverType);
							}).toArray(Element[]::new)
					)
				)
			)
	    );
		
		main.getBody().addEndElement(new Script(JavaScriptUtils.getServerStartInteractAddresses().toString()));
		main.getBody().addEndElement(new Script(JavaScriptUtils.getServerStopInteractAddresses().toString()));
		main.getBody().addEndElement(new Script(JavaScriptUtils.getNodeOutputAddresses().toString()));
		main.getBody().addScript("js/index.js");
		
		setBody(main.getBody());
		setHead(main.getHead());
	}
}
