package nodeapi;

import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import model.Query;
import model.Table;
import models.GameServerTable;
import nodemain.StartUpApplication;
import server.GameServerFactory;
import utils.Utils;
import utils.servlet.HttpStatus;

@WebServlet(
		name = "ServerAdd",
		urlPatterns = "/ServerAdd",
		asyncSupported = true
)
@MultipartConfig
public class ServerAdd extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		var serverID = ApiSettings.SERVER_ID.parse(request);
		if(!Utils.optionalsPresent(serverID))
		{
			response.setStatus(HttpStatus.BAD_REQUEST.getCode());
			return;
		}
		
		Table gameServer;
		try
		{
			var gameServerOptional = Query.query(StartUpApplication.database, GameServerTable.class)
									.filter(GameServerTable.ID, serverID.get())
									.first();
			
			if(gameServerOptional.isEmpty())
			{
				response.setStatus(400);
				return;
			}
			
			gameServer = gameServerOptional.get();
			
		} catch (SQLException e)
		{
			StartUpApplication.LOGGER.error(e.getMessage());
			response.setStatus(500);
			return;
		}
		
		var generatedServer = GameServerFactory.getSpecificServer(gameServer);
		if(generatedServer == null)
		{
			response.setStatus(500);
			return;
		}
		StartUpApplication.addServer(serverID.get(), generatedServer);
		
		var fileParts = request.getParts()
							   .parallelStream()
							   .filter(p -> p.getSubmittedFileName() != null)
							   .collect(Collectors.toList());
		
		for(var p : fileParts)
		{
			var fileName = p.getSubmittedFileName();
			if(fileName.endsWith(".zip"))
			{
				FileUpload.uploadFolder(StartUpApplication.getServerFolder(gameServer), p.getInputStream());
			}
		}
	}
}
