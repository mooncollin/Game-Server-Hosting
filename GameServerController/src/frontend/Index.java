package frontend;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import model.Query;
import model.Table;
import models.GameServerTable;
import utils.ParameterURL;

@WebServlet("/Home")
public class Index extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/Home";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL getEndpoint()
	{
		var url = new ParameterURL(PARAMETER_URL);
		return url;
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		List<Table> gameServers;
		try
		{
			gameServers = Query.query(StartUpApplication.database, GameServerTable.class)
								   .all();
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var serverNames = gameServers.stream()
									 .collect(Collectors.toMap(t -> t.getColumnValue(GameServerTable.ID), t -> t.getColumnValue(GameServerTable.NAME)));
		
		var template = new frontend.templates.IndexTemplate(serverNames);
		response.setContentType("text/html");
		response.getWriter().print(template);
	}
}
