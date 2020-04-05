package nodeapi;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Query;
import models.TriggersTable;
import nodemain.StartUpApplication;
import utils.ParameterURL;
import utils.Utils;

@WebServlet("/TriggerDelete")
public class TriggerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/GameServerNode/TriggerDelete";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		ParameterURL.HTTP_PROTOCOL, "", ApiSettings.TOMCAT_HTTP_PORT, URL
	);
	
	public static ParameterURL getEndpoint(int triggerID)
	{
		var url = new ParameterURL(PARAMETER_URL);
		url.addQuery(ApiSettings.TRIGGER_ID.getName(), triggerID);
		return url;
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var id = ApiSettings.TRIGGER_ID.parse(request);
		if(!Utils.optionalsPresent(id))
		{
			response.setStatus(400);
			return;
		}
		
		try
		{
			var option = Query.query(StartUpApplication.database, TriggersTable.class)
							   .filter(TriggersTable.ID.cloneWithValue(id.get()))
							   .first();
			
			if(option.isEmpty())
			{
				response.setStatus(400);
				return;
			}
			
			var trigger = option.get();
			
			StartUpApplication.removeTrigger(trigger);
			trigger.delete(StartUpApplication.database);
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
	}
}
