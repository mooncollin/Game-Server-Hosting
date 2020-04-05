package frontend;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

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

@WebServlet("/NodesInfo")
public class NodesInfo extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/NodesInfo";
	
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
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
		
		for(var server : servers)
		{
			var nodeOwner = server.getColumnValue(GameServerTable.NODE_OWNER);
			var serverID = server.getColumnValue(GameServerTable.ID);
			var serverName = server.getColumnValue(GameServerTable.NAME);
			var serverTypeName = StartUpApplication.serverTypesToNames.get(StartUpApplication.serverTypes.get(serverID));
			nodesToServers.get(nodeOwner).add(new ServerInfo(serverID, serverName, serverTypeName));
		}
		
		var nodeUsageAddresses = new LinkedList<String>();
		for(var ipAddress : StartUpApplication.nodeIPAddresses.values())
		{
			var url = nodeapi.NodeUsage.getEndpoint();
			url.setHost(ipAddress);
			nodeUsageAddresses.add(url.getURL());
		}
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("nodesToServers", nodesToServers);
		context.put("javascriptUtils", JavaScriptUtils.class);
		context.put("nodeNames", StartUpApplication.NODE_NAMES);
		context.put("nodeUsageAddresses", nodeUsageAddresses);
		
		var template = Velocity.getTemplate("nodesinfo.vm");
		template.merge(context, response.getWriter());
	}
}
