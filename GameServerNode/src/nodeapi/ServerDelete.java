package nodeapi;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nodemain.StartUpApplication;

@WebServlet(
		name = "ServerDelete",
		urlPatterns = "/ServerDelete",
		asyncSupported = true
)
public class ServerDelete extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		
		if(serverID.isEmpty())
		{
			response.setStatus(400);
			return;
		}
		
		var serverFound = StartUpApplication.getServer(serverID.get());
		if(serverFound == null)
		{
			response.setStatus(400);
			return;
		}
		
		FileDelete.deleteDirectoryOrFile(serverFound.getFolderLocation());
		StartUpApplication.removeServer(serverID.get());
	}
}
