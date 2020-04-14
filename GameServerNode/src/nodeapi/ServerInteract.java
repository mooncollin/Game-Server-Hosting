package nodeapi;

import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import nodemain.StartUpApplication;
import utils.Utils;
import utils.servlet.Endpoint;
import utils.servlet.HttpStatus;
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
	
	public static ParameterURL postEndpoint(int id)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID.getName(), id);
		return url;
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		
		if(!Utils.optionalsPresent(serverID))
		{
			response.setStatus(HttpStatus.BAD_REQUEST.getCode());
			return;
		}
		
		var foundServer = StartUpApplication.getServer(serverID.get());
		
		if(foundServer == null)
		{
			response.setStatus(HttpStatus.BAD_REQUEST.getCode());
			return;
		}
		
		JSONObject requestObj;
		try
		{
			requestObj = (JSONObject) JSONValue.parseWithException((request.getReader()));
		} catch (ParseException | ClassCastException e)
		{
			request.getReader().transferTo(new OutputStreamWriter(System.out));
			response.setStatus(HttpStatus.BAD_REQUEST.getCode());
			return;
		}
		
		var responseObj = new JSONObject();
		var httpStatus = foundServer.getCommandHandler().command(requestObj, responseObj);
		response.setStatus(httpStatus.getCode());
		response.setContentType("application/json");
		responseObj.writeJSONString(response.getWriter());
	}
}
