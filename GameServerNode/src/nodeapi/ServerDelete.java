package nodeapi;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nodemain.StartUpApplication;
import utils.servlet.Endpoint;
import utils.servlet.ParameterURL;

@WebServlet("/ServerDelete")
public class ServerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/ServerDelete";
	
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
		
		if(serverID.isEmpty())
		{
			response.setStatus(400);
			return;
		}
		
		var serverFound = StartUpApplication.getServer(serverID.get());
		if(serverFound == null)
		{
			response.setStatus(400);
			return;
		}
		
		FileDelete.deleteDirectoryOrFile(serverFound.getFolderLocation());
		StartUpApplication.removeServer(serverID.get());
	}
}
