package nodeapi;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Query;
import model.Table;
import models.TriggersTable;
import nodemain.StartUpApplication;
import utils.Utils;

@WebServlet(
		name = "TriggerEdit",
		urlPatterns = "/TriggerEdit",
		asyncSupported = true
)
public class TriggerEdit extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
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
			
			Table trigger;
			
			if(option.isEmpty())
			{
				response.setStatus(400);
				return;
			}
			else
			{
				trigger = option.get();
			}
			
			StartUpApplication.addTrigger(trigger);
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.error(e.getMessage());
			response.setStatus(500);
			return;
		}
	}
}
