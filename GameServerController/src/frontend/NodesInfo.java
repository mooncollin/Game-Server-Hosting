package frontend;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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
 * The frontend for displaying information about the various nodes.
 * @author Collin
 *
 */
@WebServlet(
		name = "NodesInfo",
		urlPatterns = "/NodesInfo",
		asyncSupported = true
)
public class NodesInfo extends HttpServlet
{	
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var nodesToServers = new HashMap<String, List<ServerInfo>>();
		
		for(var nodeName : StartUpApplication.NODE_NAMES)
		{
			nodesToServers.put(nodeName, new LinkedList<ServerInfo>());
		}
		
		List<Table> servers;
		try
		{
			servers = Query.query(StartUpApplication.database, GameServerTable.class)
							   .all();
			
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.error(e.getMessage());
			response.setStatus(500);
			return;
		}
		
		for(var server : servers)
		{
			var nodeOwner = server.getColumnValue(GameServerTable.NODE_OWNER);
			var serverID = server.getColumnValue(GameServerTable.ID);
			var serverName = server.getColumnValue(GameServerTable.NAME);
			var serverTypeName = server.getColumnValue(GameServerTable.SERVER_TYPE);
			var module = StartUpApplication.getModule(serverTypeName);
			
			nodesToServers.get(nodeOwner).add(new ServerInfo(serverID, serverName, serverTypeName, module));
		}
		
		var nodeUsageAddresses = new LinkedList<String>();
		for(var nodeName : StartUpApplication.NODE_NAMES)
		{
			var url = nodeapi.Endpoints.NODE_USAGE.open();
			var ipAddress = StartUpApplication.getNodeIPAddress(nodeName);
			url.setHost(ipAddress);
			nodeUsageAddresses.add(url.getURL());
		}
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("nodesToServers", nodesToServers);
		context.put("nodeNames", StartUpApplication.NODE_NAMES);
		context.put("nodeUsageAddresses", nodeUsageAddresses);
		context.put("serverCommandEndpoint", Templates.getServerCommandEndpoint());
		context.put("nodeOutputAddresses", StartUpApplication.getNodeOutputAddresses("running"));
		
		var template = Velocity.getTemplate("nodesinfo.vm");
		template.merge(context, response.getWriter());
	}
}
