package nodeapi;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Query;
import model.Table;
import models.GameServerTable;
import nodemain.StartUpApplication;
import utils.ParameterURL;

@WebServlet("/ServerDelete")
public class ServerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/ServerDelete";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		ParameterURL.HTTP_PROTOCOL, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL getEndpoint(int id)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID.getName(), id);
		return url;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
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
		
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.ID, serverID.get())
								  .first();
			
			Table gameServer;
			
			if(option.isEmpty())
			{
				response.setStatus(400);
				return;
			}
			else
			{
				gameServer = option.get();
			}
			
			gameServer.delete(StartUpApplication.database);
		}
		catch(SQLException e)
		{
			response.setStatus(500);
			return;
		}
		
		FileDelete.deleteDirectoryOrFile(serverFound.getFolderLocation());
		StartUpApplication.removeServer(serverID.get());
	}
}
