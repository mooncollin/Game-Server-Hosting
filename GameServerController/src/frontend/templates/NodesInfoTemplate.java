package frontend.templates;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import attributes.Attributes;
import backend.main.StartUpApplication;
import frontend.javascript.JavaScriptUtils;
import html.Element;
import model.Table;
import models.GameServerTable;
import tags.Div;
import tags.H1;
import tags.HR;
import tags.LI;
import tags.Script;
import tags.TableBody;
import tags.TableHead;
import tags.TableHeader;
import tags.UL;
import util.Template;

public class NodesInfoTemplate extends Template
{
	@SuppressWarnings("unchecked")
	public NodesInfoTemplate(Map<String, List<Table>> nodeNamesToServers)
	{
		var mainTemplate = Templates.getMainTemplate();
		var content = Div.class.cast(mainTemplate.getBody().getElementById("content"));
		content.getStyles().setBackgroundImage("url('images/blackdots.jpg')");
		
		content.addElements
		(
			new Div()
				.addClasses("container", "mt-5")
				.addElements
				(
					new UL()
						.addClasses("list-group")
						.addElements
						(
							Stream.of(StartUpApplication.NODE_NAMES)
								  .map(nodeName -> {
									  return new LI()
										  	 .addClasses("bg-dark", "shadow-lg", "text-light", "list-group-item")
										  	 .addElements
										  	 (
										  		new H1(nodeName).addClasses("text-center"),
										  		new HR(),
										  		new Div()
										  			.addAttributes
										  			(
										  				Attributes.Width.makeAttribute(200),
										  				Attributes.Height.makeAttribute(200)
										  			)
										  			.addElements
										  			(
										  				Templates.generateNodeUsageGraph(nodeName)
										  			),
										  		new tags.Table()
										  			.addClasses("table", "table-striped", "table-hover", "table-dark")
										  			.addElements
										  			(
										  				new TableHeader()
										  					.addElements
										  					(
										  						new TableHead("Name")
										  							.addAttributes(Attributes.makeAttribute("scope", "col")),
										  						new TableHead("Type")
										  							.addAttributes(Attributes.makeAttribute("scope", "col")),
										  						new TableHead("Status")
										  							.addAttributes(Attributes.makeAttribute("scope", "col")),
										  						new TableHead("Options")
										  							.addAttributes(Attributes.makeAttribute("scope", "col"))
										  					),
										  				new TableBody()
										  					.addElements
										  					(
										  						nodeNamesToServers.get(nodeName)
							  										  .stream()
							  										  .map(server -> {
							  											  var serverName = server.getColumnValue(GameServerTable.NAME);
							  											  var serverID = server.getColumnValue(GameServerTable.ID);
							  											  var serverType = StartUpApplication.serverTypes.get(serverID);
							  											  
							  											  return Templates.createServerTableRow(serverID, serverName, serverType);
							  										  }).toArray(Element[]::new)
										  					)
										  			)
										  	 );
								  }).toArray(Element[]::new)
						)
				)
		);
		
		mainTemplate.getBody().addEndElement(Templates.generateNodeNamesWithUsageAddresses());
		mainTemplate.getBody().addEndElement(new Script(JavaScriptUtils.getServerStartInteractAddresses().toString()));
		mainTemplate.getBody().addEndElement(new Script(JavaScriptUtils.getServerStopInteractAddresses().toString()));
		mainTemplate.getBody().addEndElement(new Script(JavaScriptUtils.getNodeOutputAddresses().toString()));
		mainTemplate.getBody().addScript("js/moment.js");
		mainTemplate.getBody().addScript("js/Chart.min.js");
		mainTemplate.getBody().addScript("js/chartjs-plugin-streaming.js");
		mainTemplate.getBody().addScript("js/nodeUsage.js");
		mainTemplate.getBody().addScript("js/index.js");
		
		setBody(mainTemplate.getBody());
		setHead(mainTemplate.getHead());
	}
}
