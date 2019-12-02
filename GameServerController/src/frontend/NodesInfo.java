package frontend;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.templates.NodesInfoTemplate;
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
		Map<String, List<Table>> nodesToServers = new HashMap<String, List<Table>>();
		
		for(var nodeName : StartUpApplication.NODE_NAMES)
		{
			nodesToServers.put(nodeName, new LinkedList<Table>());
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
			nodesToServers.get(nodeOwner).add(server);
		}
		
		var template = new NodesInfoTemplate(nodesToServers);
		
		response.setContentType("text/html");
		response.getWriter().print(template);
	}
}
