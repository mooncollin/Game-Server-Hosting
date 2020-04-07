package nodeapi;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nodemain.StartUpApplication;
import utils.Utils;
import utils.servlet.Endpoint;
import utils.servlet.ParameterURL;

@WebServlet("/ServerInteract")
public class ServerInteract extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/ServerInteract";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
			Endpoint.Protocol.HTTP, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL getEndpoint(int id, String command)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID.getName(), id);
		url.addQuery(ApiSettings.COMMAND.getName(), command);
		return url;
	}
	
	public static ParameterURL postEndpoint(int id, String command)
	{
		return getEndpoint(id, command);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var command = ApiSettings.COMMAND.parse(request);
		
		if(!Utils.optionalsPresent(serverID, command))
		{
			response.setStatus(400);
			return;
		}
		
		var foundServer = StartUpApplication.getServer(serverID.get());
		
		if(foundServer == null)
		{
			response.setStatus(400);
			return;
		}
		
		if(!foundServer.getCommandHandler().commandGET(command.get(), request, response))
		{
			response.setStatus(400);
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		var command = ApiSettings.COMMAND.parse(request);
		
		if(!Utils.optionalsPresent(serverID, command))
		{
			response.setStatus(400);
			return;
		}
		
		var foundServer = StartUpApplication.getServer(serverID.get());
		
		if(foundServer == null)
		{
			response.setStatus(400);
			return;
		}
		
		if(!foundServer.getCommandHandler().commandPOST(command.get(), request, response))
		{
			response.setStatus(400);
		}
	}
}
