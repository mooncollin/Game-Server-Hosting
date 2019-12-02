package frontend;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.templates.GameServerConsoleTemplate;
import model.Query;
import model.Table;
import models.GameServerTable;
import models.TriggersTable;
import nodeapi.ApiSettings;
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/GameServerConsole")
public class GameServerConsole extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameServerConsole";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL getEndpoint(int id)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.SERVER_ID_PARAMETER, id);
		return url;
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = Utils.fromString(Integer.class, request.getParameter(ApiSettings.SERVER_ID_PARAMETER));
		
		if(serverID == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		var serverType = StartUpApplication.serverTypes.get(serverID);
		var serverAddress = StartUpApplication.serverIPAddresses.get(serverID);
		
		if(serverType == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		String serverName;
		List<Table> triggers;
		
		try
		{
			var serverOptional = Query.query(StartUpApplication.database, GameServerTable.class)
									  .filter(GameServerTable.ID.cloneWithValue(serverID))
									  .first();
			
			if(serverOptional.isPresent())
			{
				serverName = serverOptional.get().getColumnValue(GameServerTable.NAME);
			}
			else
			{
				response.sendRedirect(Index.URL);
				return;
			}
			
			triggers = Query.query(StartUpApplication.database, TriggersTable.class)
								.filter(TriggersTable.SERVER_OWNER.cloneWithValue(serverID))
								.all();
			
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var template = new GameServerConsoleTemplate(serverAddress, serverType, serverID, serverName, triggers);
		
		response.setContentType("text/html");
		response.getWriter().print(template);
	}
}
