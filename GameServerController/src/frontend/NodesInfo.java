package frontend;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.ControllerProperties;
import backend.main.StartUpApplication;
import html.CompoundElement;
import html.Element;
import model.Table;
import models.GameServerTable;
import tags.Script;
import util.Template;
import utils.Utils;

@WebServlet("/NodesInfo")
public class NodesInfo extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/NodesInfo";

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		Template template = Templates.getMainTemplate();
		
		CompoundElement content = (CompoundElement) template.getBody().getElementById("content");
		content.setAttribute("style", "background-image: url('images/blackdots.jpg')");
		
		CompoundElement container = new CompoundElement("div");
		container.addClasses("container", "mt-5");
		
		CompoundElement nodeInformationList = new CompoundElement("ul");
		nodeInformationList.addClasses("list-group");

		String nodeAddresses = "var nodeAddresses=[";
		
		String[] nodeNames = ControllerProperties.NODE_NAMES.split(",");
		
		String serverNames = "var serverNames=[";
		
		for(String nodeName : nodeNames)
		{			
			CompoundElement nodeInfoItem = new CompoundElement("li");
			nodeInfoItem.addClasses("bg-dark", "shadow-lg", "text-light");
			CompoundElement header = new CompoundElement("h1", nodeName);
			header.addClass("text-center");
			nodeInfoItem.addClasses("list-group-item");

			nodeInfoItem.addElement(header);
			nodeInfoItem.addElement(new Element("hr"));
			
			CompoundElement canvasContainer = new CompoundElement("div");
			canvasContainer.setAttribute("width", 200);
			canvasContainer.setAttribute("height", 200);
			
			canvasContainer.addElement(Templates.generateNodeUsageGraph(nodeName));

			nodeInfoItem.addElement(canvasContainer);
			nodeInformationList.addElement(nodeInfoItem);
			
			
			CompoundElement table = new CompoundElement("table");
			table.addClasses("table", "table-striped", "table-hover", "table-dark");
			
			CompoundElement tableHeader = new CompoundElement("thead");
			CompoundElement tableHeadRow = new CompoundElement("tr");
			
			CompoundElement nameHead = new CompoundElement("th", "Name");
			nameHead.setAttribute("scope", "col");
			
			CompoundElement typeHead = new CompoundElement("th", "Type");
			typeHead.setAttribute("scope", "col");
			
			CompoundElement runningHead = new CompoundElement("th", "Status");
			runningHead.setAttribute("scope", "col");
			
			CompoundElement optionsHead = new CompoundElement("th", "Options");
			optionsHead.setAttribute("scope", "col");
			
			tableHeadRow.addElement(nameHead);
			tableHeadRow.addElement(typeHead);
			tableHeadRow.addElement(runningHead);
			tableHeadRow.addElement(optionsHead);
			tableHeader.addElement(tableHeadRow);
			
			table.addElement(tableHeader);
			
			CompoundElement tableBody = new CompoundElement("tbody");
			table.addElement(tableBody);
			
			List<Table> servers;
			try
			{
				servers = new GameServerTable().query(StartUpApplication.database)
												   .filter(GameServerTable.NODE_OWNER.cloneWithValue(nodeName))
												   .all();
			} catch (SQLException e)
			{
				response.setStatus(500);
				return;
			}
			
			for(var server : servers)
			{
				var serverInfo = StartUpApplication.getServerInfo().get(server.getColumn(GameServerTable.NAME).getValue());
				if(serverInfo != null)
				{
					Class<?> serverType = serverInfo.getFirst();
					String nodeAddress = serverInfo.getSecond();
					
					nodeAddresses += String.format("'ws://%s/Output?name=%s&mode=running',", nodeAddress, Utils.encodeURL((String) server.getColumn(GameServerTable.NAME).getValue()));
					serverNames += String.format("'%s',", server.getColumn(GameServerTable.NAME).getValue());
					
					tableBody.addElement(Templates.createServerTableRow((String) server.getColumn(GameServerTable.NAME).getValue(), serverType));
				}
			}
			
			nodeInfoItem.addElement(table);
		}
		
		nodeAddresses += "];";
		serverNames += "];";
		
		template.getBody().addEndElement(Templates.generateNodeNamesWithUsageAddresses());
		template.getBody().addEndElement(new Script(nodeAddresses + serverNames));
		template.getBody().addScript("js/moment.js");
		template.getBody().addScript("js/Chart.min.js");
		template.getBody().addScript("js/chartjs-plugin-streaming.js");
		template.getBody().addScript("js/nodeUsage.js");
		template.getBody().addScript("js/index.js");
		container.addElement(nodeInformationList);
		content.addElement(container);
		
		response.setContentType("text/html");
		response.getWriter().print(template);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
	}
}
