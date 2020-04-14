package frontend;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import backend.main.StartUpApplication;
import frontend.templates.Templates.ModuleInfo;
import model.Query;
import models.GameModuleTable;

/**
 * The frontend for displaying all the currently loaded game types.
 * @author Collin
 *
 */
@WebServlet(
		name = "GameTypes",
		urlPatterns = "/GameTypes",
		asyncSupported = true
)
public class GameTypes extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var modules = new LinkedList<ModuleInfo>();
		
		try
		{
			var query = Query.query(StartUpApplication.database, GameModuleTable.class)
							 .all();
			
			for(var row : query)
			{
				modules.add(new ModuleInfo(row.getColumnValue(GameModuleTable.NAME), new String((byte[]) row.getColumnValue(GameModuleTable.ICON.getName()))));
			}
		}
		catch(SQLException e)
		{
			StartUpApplication.LOGGER.error(String.format("Error retrieving modules:\n%s", e.getMessage()));
		}
		
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		context.put("modules", modules);
		
		var template = Velocity.getTemplate("gameTypes.vm");
		template.merge(context, response.getWriter());
	}
}
