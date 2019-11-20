package api;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.StartUpApplication;
import model.Query;
import model.Table;
import models.GameServerTable;

@WebServlet("/ServerDelete")
public class ServerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var name = request.getParameter("name");
		
		if(name == null)
		{
			response.setStatus(400);
			return;
		}
		
		var serverFound = StartUpApplication.getServer(name);
		if(serverFound == null)
		{
			response.setStatus(400);
			return;
		}
		
		try
		{
			var option = Query.query(StartUpApplication.database, GameServerTable.class)
								  .filter(GameServerTable.NAME.cloneWithValue(name))
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
		StartUpApplication.removeServer(serverFound);
	}
}
