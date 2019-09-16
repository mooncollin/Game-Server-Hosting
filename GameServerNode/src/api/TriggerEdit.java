package api;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import main.StartUpApplication;
import model.Model;

@WebServlet("/TriggerEdit")
public class TriggerEdit extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String idStr = request.getParameter("id");
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
		
		List<models.Triggers> triggers = Model.getAll(models.Triggers.class, "id=?", id);
		if(triggers.isEmpty())
		{
			response.setStatus(400);
			return;
		}
		
		models.Triggers trigger = triggers.get(0);
		StartUpApplication.addTrigger(trigger);
	}
}
