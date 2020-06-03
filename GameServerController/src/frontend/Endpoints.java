package frontend;

import java.util.List;

import backend.api.GameServerDelete;
import backend.api.GameServerFileDelete;
import backend.api.GameServerFileDeleteMultiple;
import backend.api.GameServerFileDownload;
import backend.api.GameServerFileRename;
import backend.api.GameServerNewFolder;
import backend.api.GameServerTriggerAdd;
import backend.api.GameServerTriggerDelete;
import backend.api.GameServerTriggerEdit;
import backend.api.ServerInteract;
import backend.main.StartUpApplication;
import nodeapi.ApiSettings;
import utils.servlet.HttpEndpoint;
import utils.servlet.ParameterURL;

public class Endpoints
{
	private Endpoints() {}
	
	/**
	 * The HttpEndpoint for the index page.
	 */
	public static final HttpEndpoint INDEX = new HttpEndpoint(Index.class)
	{
		/**
		 * Post is not supported.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * Gets the default index url.
		 */
		@Override
		public ParameterURL get(Object... values)
		{
			return getRequestURL();
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for the nodes info page.
	 */
	public static final HttpEndpoint NODES_INFO = new HttpEndpoint(NodesInfo.class)
	{
		/**
		 * Post is not supported
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * Gets the default node info url.
		 */
		@Override
		public ParameterURL get(Object... values)
		{
			return getRequestURL();
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for adding a game server
	 */
	public static final HttpEndpoint GAME_SERVER_ADD = new HttpEndpoint(GameServerAdd.class)
	{
		/**
		 * Used for adding a new game server.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			var serverName = (String) values[0];
			var execName = (String) values[1];
			var nodeName = (String) values[2];
			var type = (String) values[3];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_NAME.getName(), serverName);
			url.addQuery(ApiSettings.EXECUTABLE_NAME.getName(), execName);
			url.addQuery(ApiSettings.NODE_NAME.getName(), nodeName);
			url.addQuery(ApiSettings.SERVER_TYPE.getName(), type);
			
			return url;
		}
		
		/**
		 * Gets the default game server add url.
		 */
		@Override
		public ParameterURL get(Object... values)
		{
			return getRequestURL();
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for a server's console.
	 */
	public static final HttpEndpoint GAME_SERVER_CONSOLE = new HttpEndpoint(GameServerConsole.class)
	{
		/**
		 * Post is not supported.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * Gets the url with the given server id.
		 */
		@Override
		public ParameterURL get(Object... values)
		{	
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), values[0]);
			
			return url;
		}
	}.relative(StartUpApplication.SERVLET_PATH);

	/**
	 * The HttpEndpoint for a server's files.
	 */
	public static final HttpEndpoint GAME_SERVER_FILES = new HttpEndpoint(GameServerFiles.class)
	{
		/**
		 * Used getting the add file url.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			var folder = (String) values[2];
			
			var url = get(values);
			if(folder != null)
			{
				url.addQuery(ApiSettings.FOLDER.getName(), folder);
			}
			
			return url;
		}
		
		/**
		 * Used for getting the frontend page url.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public ParameterURL get(Object... values)
		{
			var serverID = (int) values[0];
			var directories = (List<String>) values[1];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
			return url;
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for a server's settings.
	 */
	public static final HttpEndpoint GAME_SERVER_SETTINGS = new HttpEndpoint(GameServerSettings.class)
	{
		/**
		 * Used to get the url for posting new server settings.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			return get(values);
		}
		
		/**
		 * Used to get the url for the settings page.
		 */
		@Override
		public ParameterURL get(Object... values)
		{
			var serverID = (int) values[0];
					
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			return url;
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for adding a new game type.
	 */
	public static final HttpEndpoint GAME_TYPE_ADD = new HttpEndpoint(GameTypeAdd.class)
	{
		/**
		 * Gets the url for posting a new game type.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			return getRequestURL();
		}
		
		/**
		 * Gets the url for the frontend.
		 */
		@Override
		public ParameterURL get(Object... values)
		{	
			return getRequestURL();
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for listing all the game types.
	 */
	public static final HttpEndpoint GAME_TYPES = new HttpEndpoint(GameTypes.class)
	{
		/**
		 * Post is not supported.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * Gets the url for the frontend.
		 */
		@Override
		public ParameterURL get(Object... values)
		{	
			return getRequestURL();
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for deleting a game server.
	 */
	public static final HttpEndpoint GAME_SERVER_DELETE = new HttpEndpoint(GameServerDelete.class)
	{
		/**
		 * Post is not supported.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * The url for deleting a game server.
		 */
		@Override
		public ParameterURL get(Object... values)
		{
			var serverID = (int) values[0];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			
			return url;
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for deleting a file on a game server.
	 */
	public static final HttpEndpoint GAME_SERVER_FILE_DELETE = new HttpEndpoint(GameServerFileDelete.class)
	{
		/**
		 * Post is not supported.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * The url for deleting a file.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public ParameterURL get(Object... values)
		{
			var serverID = (int) values[0];
			var directories = (List<String>) values[1];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
			return url;
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for deleting multiple files.
	 */
	public static final HttpEndpoint GAME_SERVER_FILE_DELETE_MULTIPLE = new HttpEndpoint(GameServerFileDeleteMultiple.class)
	{
		/**
		 * Post is not supported.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * The url for deleting multiple game server files.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public ParameterURL get(Object... values)
		{
			var serverID = (int) values[0];
			var directories = (List<String>) values[1];
			
			var url = getRequestURL();
			
			if(values[2] != null)
			{
				var files = (String) values[2];
				url.addQuery(ApiSettings.FILES.getName(), files);
			}
			
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
			return url;
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for downloading a file from a game server.
	 */
	public static final HttpEndpoint GAME_SERVER_FILE_DOWNLOAD = new HttpEndpoint(GameServerFileDownload.class)
	{
		/**
		 * Does the same thing as the get method.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			return get(values);
		}
		
		/**
		 * The url for downloading a file.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public ParameterURL get(Object... values)
		{
			var serverID = (int) values[0];
			var directories = (List<String>) values[1];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
			return url;
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for renaming a file on a game server.
	 */
	public static final HttpEndpoint GAME_SERVER_FILE_RENAME = new HttpEndpoint(GameServerFileRename.class)
	{
		/**
		 * Post is not supported.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * Gets the url for renaming a file.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public ParameterURL get(Object... values)
		{
			var serverID = (int) values[0];
			var directories = (List<String>) values[1];
			var rename = (String) values[2];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
			url.addQuery(ApiSettings.RENAME.getName(), rename);
			
			return url;
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for creating a new folder on a game server.
	 */
	public static final HttpEndpoint GAME_SERVER_NEW_FOLDER = new HttpEndpoint(GameServerNewFolder.class)
	{
		/**
		 * Post is not supported.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * Gets the url for creating a new folder.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public ParameterURL get(Object... values)
		{
			var serverID = (int) values[0];
			var directories = (List<String>) values[1];
//			var newFolder = (String) values[2];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
//			url.addQuery(ApiSettings.NEW_FOLDER.getName(), newFolder);
			
			return url;
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for adding a trigger to a game server.
	 */
	public static final HttpEndpoint GAME_SERVER_TRIGGER_ADD = new HttpEndpoint(GameServerTriggerAdd.class)
	{
		/**
		 * The url for adding a trigger.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			var serverID = (int) values[0];
			
			var url = getRequestURL();
			if(values.length == 5)
			{
				var value = (String) values[1];
				var command = (String) values[2];
				var action = (String) values[3];
				var type = (String) values[4];
				
				url.addQuery(ApiSettings.TRIGGER_VALUE.getName(), value);
				url.addQuery(ApiSettings.TRIGGER_TYPE.getName(), type);
				url.addQuery(ApiSettings.TRIGGER_COMMAND.getName(), command);
				url.addQuery(ApiSettings.TRIGGER_ACTION.getName(), action);
			}
			
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			return url;
			
		}
		
		/**
		 * Get is not supported.
		 */
		@Override
		public ParameterURL get(Object... values)
		{
			throw new UnsupportedOperationException();
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for deleting a trigger from a game server.
	 */
	public static final HttpEndpoint GAME_SERVER_TRIGGER_DELETE = new HttpEndpoint(GameServerTriggerDelete.class)
	{
		/**
		 * Post is not supported.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * The url for deleting a trigger.
		 */
		@Override
		public ParameterURL get(Object... values)
		{
			var serverID = (int) values[0];
			var triggerID = (int) values[1];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			url.addQuery(ApiSettings.TRIGGER_ID.getName(), triggerID);
			return url;
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for editing a trigger.
	 */
	public static final HttpEndpoint GAME_SERVER_TRIGGER_EDIT = new HttpEndpoint(GameServerTriggerEdit.class)
	{
		/**
		 * The url for editing a trigger.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			var serverID = (int) values[0];
			var triggerID = (int) values[1];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			url.addQuery(ApiSettings.TRIGGER_ID.getName(), triggerID);
			return url;
		}
		
		/**
		 * Get is not supported.
		 */
		@Override
		public ParameterURL get(Object... values)
		{
			throw new UnsupportedOperationException();
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for interacting with a server through the CommandHandler framework.
	 */
	public static final HttpEndpoint SERVER_INTERACT = new HttpEndpoint(ServerInteract.class)
	{
		/**
		 * Get is not supported.
		 */
		@Override
		public ParameterURL get(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		/**
		 * The url for interacting with a server.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			var url = getRequestURL();
			if(values.length > 0)
			{
				var serverID = (int) values[0];
				url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			}
			if(values.length > 1)
			{
				var command = (String) values[1];
				if(!command.isBlank())
				{
					url.addQuery(ApiSettings.COMMAND.getName(), command);
				}
			}
			
			return url;
		}
	}.relative(StartUpApplication.SERVLET_PATH);
	
	/**
	 * The HttpEndpoint for the GameServerUIHandler framework.
	 */
	public static final HttpEndpoint MODULE_SERVLETS = new HttpEndpoint(GameServerServlets.class)
	{
		/**
		 * The url to get custom UI pages from modules.
		 */
		@Override
		public ParameterURL get(Object... values)
		{
			var moduleName = (String) values[0];
			var servletName = (String) values[1];
			var serverID = (int) values[2];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.MODULE_NAME.getName(), moduleName);
			url.addQuery(ApiSettings.MODULE_SERVLET_NAME.getName(), servletName);
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			return url;
		}
		
		/**
		 * Does the same thing as the get method.
		 */
		@Override
		public ParameterURL post(Object... values)
		{
			return get(values);
		}
	}.relative(StartUpApplication.SERVLET_PATH);
}
