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

@WebServlet("/GameServerConsole")
public class GameServerConsole extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerController/GameServerConsole";
	
	public static String getEndpoint(int id)
	{
		return String.format("%s?id=%d", URL, id);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverStr = request.getParameter("id");
		int serverID;
		
		if(serverStr == null)
		{
			response.sendRedirect(Index.URL);
			return;
		}
		
		try
		{
			serverID = Integer.parseInt(serverStr);
		}
		catch(NumberFormatException e)
		{
			response.setStatus(400);
			return;
		}
		
		var serverType = StartUpApplication.serverTypes.get(serverID);
		var serverAddress = StartUpApplication.serverAddresses.get(serverID);
		
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
