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
		addServer.addClasses("rounded-circle", "bg-light", "p-2", "float-right");
		addServer.setHref(GameServerAdd.URL);
		addServer.setID("addServerLink");
		CompoundElement addServerIcon = new CompoundElement("i");
		addServerIcon.addClasses("fas", "fa-plus");
		addServerIcon.setAttribute("data-toggle", "tooltip");
		addServerIcon.setAttribute("data-placement", "bottom");
		addServerIcon.setTitle("Add game server");
		addServer.addElement(addServerIcon);
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
