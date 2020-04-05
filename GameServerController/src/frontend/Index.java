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

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import backend.main.StartUpApplication;
import frontend.javascript.JavaScriptUtils;
import frontend.templates.Templates.ServerInfo;
import model.Query;
import model.Table;
import models.GameServerTable;
import utils.ParameterURL;

@WebServlet("/Home")
public class Index extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, StartUpApplication.getUrlMapping(Index.class)
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
		
		var servers = gameServers.stream()
								 .map(s -> {
									 var serverID = s.getColumnValue(GameServerTable.ID);
									 var serverName = s.getColumnValue(GameServerTable.NAME);
									 var serverTypeName = StartUpApplication.serverTypesToNames.get(StartUpApplication.serverTypes.get(serverID));
									 return new ServerInfo(serverID, serverName, serverTypeName);
								 })
								 .collect(Collectors.toList());
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("servers", servers);
		context.put("javascriptUtils", JavaScriptUtils.class);
		
		var template = Velocity.getTemplate("index.vm");
		template.merge(context, response.getWriter());
	}
}
