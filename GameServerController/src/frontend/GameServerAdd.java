package frontend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import api.minecraft.MinecraftServer;
import backend.api.ServerInteract;
import backend.main.ControllerProperties;
import backend.main.StartUpApplication;
import forms.Form;
import forms.TextField;
import html.CompoundElement;
import html.Element;
import model.Query;
import model.TableTemp;
import models.GameServerTable;
import models.NodeTable;
import server.GameServer;
import tags.Button;
import utils.Pair;
import util.Template;

@WebServlet("/GameServerAdd")
@MultipartConfig
public class GameServerAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerAdd";
	
	private static final Pattern SERVER_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_  ]+");
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		List<TableTemp> nodes;
		try
		{
			nodes = Query.query(StartUpApplication.database, NodeTable.class)
							 .all();
		} catch (SQLException e1)
		{
			response.setStatus(500);
			return;
		}
		
		Template template = Templates.getMainTemplate();
		CompoundElement content = (CompoundElement) template.getBody().getElementById("content");
		content.setStyle("background-image: url('images/material-back.jpeg')");
		
		CompoundElement innerContent = new CompoundElement("div");
		innerContent.addClasses("ml-5", "mt-5");
		innerContent.setID("inner-content");
		
		CompoundElement header = new CompoundElement("h1", "Add Game Server");
		header.addClass("text-light");
		
		innerContent.addElement(header);
		Element line = new Element("hr");
		line.addClasses("border", "border-light");
		innerContent.addElement(line);
		
		Form form = new Form();
		form.setMethod("POST");
		form.setEnctype("multipart/form-data");
		
		CompoundElement nameGroup = BootstrapTemplates.settingsInput(TextField.class, "Server Name", "name", "Enter server name", "", "This name must be unique. Cannot contain special characters.", true, false);
		((TextField) nameGroup.getElementById("name")).setPattern(SERVER_NAME_PATTERN.pattern());
		form.addElement(nameGroup);
		
		CompoundElement execNameGroup = BootstrapTemplates.settingsInput(TextField.class, "Enter Server Executable Name", "execName", "Enter executable name", "", "This will be the file executed when the server needs to start.", true, false);
		form.addElement(execNameGroup);
		
		CompoundElement nodeSelectGroup = new CompoundElement("li");
		nodeSelectGroup.addClasses("form-group", "form-inline", "list-group-item");
		CompoundElement nodeLabel = new CompoundElement("label", "Select Node");
		nodeLabel.addClasses("d-inline-block", "w-25", "align-middle");
		nodeSelectGroup.addElement(nodeLabel);
		CompoundElement nodeSelect = new CompoundElement("select");
		nodeSelect.addClass("form-control");
		nodeSelect.setID("node");
		nodeSelect.setAttribute("name", "node");
		nodeSelect.setAttribute("required", "");
		nodeSelect.setAttribute("onchange", "changeNode()");
		
		for(var nodeName : ControllerProperties.NODE_NAMES.split(","))
		{
			CompoundElement option = new CompoundElement("option", nodeName);
			for(var node : nodes)
			{
				if(node.getColumnValue(NodeTable.NAME).equals(nodeName))
				{
					option.setAttribute("totalram", String.valueOf(node.getColumnValue(NodeTable.MAX_RAM_ALLOWED)));
					try
					{
						option.setAttribute("reservedram", StartUpApplication.getNodeReservedRam(nodeName));
					} catch (SQLException e)
					{
						response.setStatus(500);
						return;
					}
					break;
				}
			}
			nodeSelect.addElement(option);
		}
		nodeSelectGroup.addElement(nodeSelect);
		CompoundElement nodeSmallLabel = new CompoundElement("small");
		nodeSmallLabel.addClasses("form-text", "text-muted");
		nodeSelectGroup.addElement(nodeSmallLabel);
		form.addElement(nodeSelectGroup);
		
		CompoundElement typeSelectGroup = new CompoundElement("li");
		typeSelectGroup.addClasses("form-group", "form-inline", "list-group-item");
		CompoundElement typeLabel = new CompoundElement("label", "Select Server Type");
		typeLabel.addClasses("d-inline-block", "w-25", "align-middle");
		typeSelectGroup.addElement(typeLabel);
		CompoundElement typeSelect = new CompoundElement("select");
		typeSelect.addClass("form-control");
		typeSelect.setID("type");
		typeSelect.setAttribute("name", "type");
		typeSelect.setAttribute("required", "");
		typeSelect.setAttribute("onchange", "changeType()");
		for(String types : GameServer.PROPERTY_NAMES_TO_TYPE.keySet())
		{
			typeSelect.addElement(new CompoundElement("option", types));
		}
		typeSelectGroup.addElement(typeSelect);
		form.addElement(typeSelectGroup);
	
		CompoundElement heapGroup = BootstrapTemplates.settingsInput(forms.Number.class, "Maximum RAM (MB)", "ramAmount", "Enter maximum amount of ram to use in MB", String.valueOf(MinecraftServer.MINIMUM_HEAP_SIZE), "RAM must be in 1024 increments", true, false);
		forms.Number heapInput = (forms.Number) heapGroup.getElementById("ramAmount");
		heapInput.setMin(MinecraftServer.MINIMUM_HEAP_SIZE);
		heapInput.setStep(1024);
		heapGroup.addClass("minecraft-type");
		form.addElement(heapGroup);
		
		CompoundElement restartGroup = BootstrapTemplates.settingsInput(forms.Checkbox.class, "Automatic Restart", "restartsUnexpected", null, "true", "This will restart the server when it stops unexpectingly", false, false);
		restartGroup.addClass("minecraft-type");
		form.addElement(restartGroup);
		
		CompoundElement fileGroup = BootstrapTemplates.settingsInput(forms.File.class, "Starting Files", "files", null, "", "The zip given will unzip in the parent directory. This is optional", false, false);
		forms.File file = (forms.File) fileGroup.getElementById("files");
		file.removeClass("form-control");
		file.addClass("form-control-file");
		file.setAccept(".zip");
		form.addElement(fileGroup);
		
		CompoundElement buttonGroup = new CompoundElement("li");
		buttonGroup.addClasses("list-group-item");
		Button submit = new Button("Submit");
		submit.addClasses("btn", "btn-primary");
		buttonGroup.addElement(submit);
		form.addElement(buttonGroup);
		
		innerContent.addElement(form);
		content.addElement(innerContent);
		template.getBody().addScript("js/addServer.js");
		response.setContentType("text/html");
		response.getWriter().print(template);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String serverName = request.getParameter("name");
		String executableName = request.getParameter("execName");
		String nodeName = request.getParameter("node");
		String type = request.getParameter("type");
		
		if(serverName == null || executableName == null || nodeName == null || type == null)
		{
			doGet(request, response);
			return;
		}
		
		if(!SERVER_NAME_PATTERN.matcher(serverName).matches())
		{
			doGet(request, response);
			return;
		}
		
		String serverAddress = null;
		for(int i = 0; i < StartUpApplication.NODE_NAMES.length; i++)
		{
			if(nodeName.equals(StartUpApplication.NODE_NAMES[i]))
			{
				serverAddress = String.format("%s:%s/%s", StartUpApplication.NODE_ADDRESSES[i], StartUpApplication.NODE_PORTS[i], ControllerProperties.NODE_EXTENSION);
				break;
			}
		}
		
		if(serverAddress == null)
		{
			doGet(request, response);
			return;
		}
		
		try
		{
			var gameServer = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.NAME.cloneWithValue(serverName))
								  .first();
			if(gameServer != null)
			{
				doGet(request, response);
				return;
			}
		}
		catch(SQLException e)
		{
			response.setStatus(500);
			return;
		}
		
		String url = String.format("http://%s/ServerAdd?name=%s&execName=%s&type=%s", serverAddress, serverName, executableName, type);
		
		if(type.equals("minecraft"))
		{
			String ramStr = request.getParameter("ramAmount");
			int ram;
			String restart = request.getParameter("restartsUnexpected");
			if(ramStr == null)
			{
				doGet(request, response);
				return;
			}
			
			try
			{
				ram = Integer.parseInt(ramStr);
			}
			catch(NumberFormatException e)
			{
				doGet(request, response);
				return;
			}
			
			if(ram < MinecraftServer.MINIMUM_HEAP_SIZE || ram % 1024 != 0)
			{
				doGet(request, response);
				return;
			}
			
			restart = restart == null ? "no" : "yes";
			
			url += String.format("&ram=%s&restart=%s", ramStr, restart);
		}
		
		final String boundary = "===" + System.currentTimeMillis() + "===";
		
		ByteArrayOutputStream zipRequest = new ByteArrayOutputStream();
		for(Part p : request.getParts())
		{
			String header = p.getHeader("Content-Disposition");
			String fileName = header.substring(header.indexOf("filename=") + "filename=".length() + 1);
			fileName = fileName.substring(0, fileName.length() - 1);
			if(fileName.endsWith(".zip"))
			{
				zipRequest.writeBytes(String.format("\r\n--%s\r\nContent-Disposition: %s\r\n\r\n", boundary, p.getHeader("Content-Disposition")).getBytes());
				p.getInputStream().transferTo(zipRequest);
			}
		}
		
		zipRequest.writeBytes(String.format("\r\n--%s--", boundary).getBytes());
		url = url.replace(" ", "+");
		HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(url))
				.header("Content-type", "multipart/form-data; boundary=" + boundary)
				.POST(BodyPublishers.ofByteArray(zipRequest.toByteArray()))
				.build();
		zipRequest.reset();
		zipRequest.close();
		try
		{
			HttpResponse<Void> httpResponse = ServerInteract.client.send(httpRequest, BodyHandlers.discarding());
			if(httpResponse.statusCode() == 200)
			{
				StartUpApplication.getServerInfo().put(serverName, new Pair<Class<? extends GameServer>, String>(MinecraftServer.class, serverAddress));
				response.sendRedirect(Index.URL);
				return;
			}
		}
		catch(InterruptedException e)
		{
			
		}
		
		doGet(request, response);
	}
}
