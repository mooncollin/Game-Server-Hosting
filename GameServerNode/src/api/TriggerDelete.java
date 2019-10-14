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
import models.TriggersTable;

@WebServlet("/TriggerDelete")
public class TriggerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
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
			var trigger = Query.query(StartUpApplication.database, TriggersTable.class)
							   .filter(TriggersTable.ID.cloneWithValue(id))
							   .first();
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
