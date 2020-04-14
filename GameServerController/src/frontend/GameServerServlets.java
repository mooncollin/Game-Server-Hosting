package frontend;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import backend.main.StartUpApplication;
import nodeapi.ApiSettings;
import utils.Utils;

/**
 * Responsible for delivering custom web pages from a given module.
 * @author Collin
 *
 */
@WebServlet(
		name = "GameServerServlets",
		urlPatterns = "/GameServerServlets",
		asyncSupported = true
)
public class GameServerServlets extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var moduleName = ApiSettings.MODULE_NAME.parse(request);
		var servletName = ApiSettings.MODULE_SERVLET_NAME.parse(request);
		
		if(!Utils.optionalsPresent(moduleName, servletName))
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		var module = StartUpApplication.getModule(moduleName.get());
		if(module == null)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		var servlet = module.gameServerUIHandler().getRegisteredServlets().get(servletName.get());
		if(servlet == null)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		try
		{
			var getMethod = servlet.getClass().getDeclaredMethod("doGet", HttpServletRequest.class, HttpServletResponse.class);
			getMethod.setAccessible(true);
			getMethod.invoke(servlet, request, response);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var moduleName = ApiSettings.MODULE_NAME.parse(request);
		var servletName = ApiSettings.MODULE_SERVLET_NAME.parse(request);
		
		if(!Utils.optionalsPresent(moduleName, servletName))
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		var module = StartUpApplication.getModule(moduleName.get());
		if(module == null)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		var servlet = module.gameServerUIHandler().getRegisteredServlets().get(servletName.get());
		if(servlet == null)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
		
		try
		{
			var postMethod = servlet.getClass().getDeclaredMethod("doPost", HttpServletRequest.class, HttpServletResponse.class);
			postMethod.setAccessible(true);
			postMethod.invoke(servlet, request, response);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			response.sendRedirect(Endpoints.INDEX.get().getURL());
			return;
		}
	}
}
