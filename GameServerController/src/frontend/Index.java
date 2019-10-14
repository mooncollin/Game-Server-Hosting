package frontend;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import html.CompoundElement;
import html.Element;
import tags.Anchor;
import tags.Script;
import util.Template;
import utils.Utils;

@WebServlet("/Home")
public class Index extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/Home";
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		Template template = Templates.getMainTemplate();
		CompoundElement content = (CompoundElement) template.getBody().getElementById("content");
		content.setStyle("background-image: url('images/material-back.jpeg')");
		
		CompoundElement innerContent = new CompoundElement("div");
		innerContent.addClasses("ml-5", "mt-5");
		innerContent.setID("inner-content");
		
		CompoundElement header = new CompoundElement("h1", "Current Servers");
		header.addClass("text-light");
		
		Anchor addServer = new Anchor();
		addServer.addClasses("rounded-circle", "bg-light", "p-3", "float-right", "addLink", "mr-3");
		addServer.setHref(GameServerAdd.URL);
		addServer.setAttribute("data-toggle", "tooltip");
		addServer.setAttribute("data-placement", "bottom");
		addServer.setTitle("Add game server");
		
		CompoundElement addServerIcon = Templates.createIcon("plus");
		addServerIcon.addClasses("mr-2");
		
		CompoundElement addServerIcon2 = Templates.createIcon("server");
		
		addServer.addElement(addServerIcon);
		addServer.addElement(addServerIcon2);
		
		Anchor addGame = new Anchor();
		addGame.addClasses("rounded-circle", "bg-light", "p-3", "float-right", "addLink");
		addGame.setHref(GameTypeAdd.URL);
		addGame.setAttribute("data-toggle", "tooltip");
		addGame.setAttribute("data-placement", "bottom");
		addGame.setTitle("Add game type");
		
		CompoundElement addGameIcon = Templates.createIcon("gamepad");
		
		addGame.addElement(addServerIcon);
		addGame.addElement(addGameIcon);
		
		header.addElement(addGame);
		header.addElement(addServer);
		
		innerContent.addElement(header);
		Element line = new Element("hr");
		line.addClasses("border", "border-light");
		innerContent.addElement(line);
		
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
		String nodeAddresses = "var nodeAddresses=[";
		String serverNames = "var serverNames=[";
		
		for(String serverName : StartUpApplication.getServerInfo().keySet())
		{
			Class<?> serverType = StartUpApplication.getServerInfo().get(serverName).getFirst();
			String nodeAddress = StartUpApplication.getServerInfo().get(serverName).getSecond();
			nodeAddresses += String.format("'ws://%s/Output?name=%s&mode=running',", nodeAddress, Utils.encodeURL(serverName));
			serverNames += String.format("'%s',", serverName);
			
			tableBody.addElement(Templates.createServerTableRow(serverName, serverType));
		}
		
		nodeAddresses += "];";
		serverNames += "];";
		
		innerContent.addElement(table);
		content.addElement(innerContent);
		template.getBody().addEndElement(new Script(nodeAddresses + serverNames));
		template.getBody().addScript("js/index.js");
		response.setContentType("text/html");
		response.getWriter().print(template);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
	}
}
