package frontend;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import backend.main.StartUpApplication;

@WebServlet(
		name = "GameTypeAdd",
		urlPatterns = "/GameTypeAdd",
		asyncSupported = true
)
@ServletSecurity(
		httpMethodConstraints = {
				@HttpMethodConstraint(value = "GET"),
				@HttpMethodConstraint(value = "POST")
		}
)
public class GameTypeAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("unused")
	private static final Pattern SERVER_TYPE_PATTERN = Pattern.compile("[A-Za-z0-9_]+");
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var context = (VelocityContext) StartUpApplication.GLOBAL_CONTEXT.clone();
		
		var template = Velocity.getTemplate("addServerType.vm");
		template.merge(context, response.getWriter());
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		
	}
}
