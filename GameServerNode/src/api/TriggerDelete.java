package api;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.StartUpApplication;
import model.Query;
import model.Table;
import models.TriggersTable;

@WebServlet("/TriggerDelete")
public class TriggerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = "/TriggerDelete";
	
	public static String getEndpoint(int triggerID)
	{
		return String.format("%s?id=%d", URL, triggerID);
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var idStr = request.getParameter("id");
		int id;
		
		if(idStr == null)
		{
			response.setStatus(400);
			return;
		}
		
		try
		{
			id = Integer.valueOf(idStr);
		}
		catch(NumberFormatException e)
		{
			response.setStatus(400);
			return;
		}
		
		try
		{
			var option = Query.query(StartUpApplication.database, TriggersTable.class)
							   .filter(TriggersTable.ID.cloneWithValue(id))
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
			
			trigger.delete(StartUpApplication.database);
			StartUpApplication.removeTrigger(trigger);
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.log(Level.SEVERE, e.getMessage());
			response.setStatus(500);
			return;
		}
	}
}
