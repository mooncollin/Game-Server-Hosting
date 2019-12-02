package frontend;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import frontend.templates.Templates;
import util.Template;
import utils.ParameterURL;

@WebServlet("/GameTypeAdd")
public class GameTypeAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	public static final String URL = StartUpApplication.SERVLET_PATH + "/GameTypeAdd";
	
	private static final ParameterURL PARAMETER_URL = new ParameterURL
	(
		null, null, null, URL
	);
	
	public static ParameterURL getEndpoint()
	{
		var url = new ParameterURL(PARAMETER_URL);
		return url;
	}
	
	private static final Pattern SERVER_TYPE_PATTERN = Pattern.compile("[A-Za-z0-9_]+");
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		Template template = Templates.getMainTemplate();
		
		response.setContentType("text/html");
		response.getWriter().print(template);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		
	}
}
