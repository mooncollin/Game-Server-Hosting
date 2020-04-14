package frontend;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import backend.main.StartUpApplication;
import frontend.templates.Templates;
import frontend.templates.Templates.ServerInfo;
import model.Query;
import model.Table;
import models.GameServerTable;

/**
 * The frontend for the home page.
 * @author Collin
 *
 */
@WebServlet(
		name = "Index",
		urlPatterns = "/Home",
		asyncSupported = true
)
public class Index extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		List<Table> gameServers;
		try
		{
			gameServers = Query.query(StartUpApplication.database, GameServerTable.class)
								   .all();
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.error(e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var servers = gameServers.parallelStream()
								 .map(s -> {
									 var serverID = s.getColumnValue(GameServerTable.ID);
									 var serverName = s.getColumnValue(GameServerTable.NAME);
									 var serverTypeName = s.getColumnValue(GameServerTable.SERVER_TYPE);
									 var module = StartUpApplication.getModule(serverTypeName);
									
									 return new ServerInfo(serverID, serverName, serverTypeName, module);
								 })
								 .collect(Collectors.toList());
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("servers", servers);
		context.put("serverCommandEndpoint", Templates.getServerCommandEndpoint());
		context.put("nodeOutputAddresses", StartUpApplication.getNodeOutputAddresses("running"));
		
		var template = Velocity.getTemplate("index.vm");
		template.merge(context, response.getWriter());
	}
}
