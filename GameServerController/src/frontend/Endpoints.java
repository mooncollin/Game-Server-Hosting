package frontend;

import java.util.List;

import backend.api.GameServerDelete;
import backend.api.GameServerFileDelete;
import backend.api.GameServerFileDeleteMultiple;
import backend.api.GameServerFileDownload;
import backend.api.GameServerFileRename;
import backend.api.GameServerTriggerAdd;
import backend.api.GameServerTriggerDelete;
import backend.api.GameServerTriggerEdit;
import backend.api.ServerInteract;
import backend.main.StartUpApplication;
import nodeapi.ApiSettings;
import utils.servlet.Endpoint;
import utils.servlet.ParameterURL;

public class Endpoints
{
	public static final Endpoint INDEX = new Endpoint(Index.class, StartUpApplication.SERVLET_PATH)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ParameterURL get(Object... values)
		{
			return getRequestURL();
		}
	};
	
	public static final Endpoint NODES_INFO = new Endpoint(NodesInfo.class, StartUpApplication.SERVLET_PATH)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ParameterURL get(Object... values)
		{
			return getRequestURL();
		}
	};
	
	public static final Endpoint GAME_SERVER_ADD = new Endpoint(GameServerAdd.class, StartUpApplication.SERVLET_PATH)
	{
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
		
		@Override
		public ParameterURL get(Object... values)
		{
			return getRequestURL();
		}
	};
	
	public static final Endpoint GAME_SERVER_CONSOLE = new Endpoint(GameServerConsole.class, StartUpApplication.SERVLET_PATH)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ParameterURL get(Object... values)
		{	
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), values[0]);
			
			return url;
		}
	};

	public static final Endpoint GAME_SERVER_FILES = new Endpoint(GameServerFiles.class, StartUpApplication.SERVLET_PATH)
	{
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
	};
			
	public static final Endpoint GAME_SERVER_SETTINGS = new Endpoint(GameServerSettings.class, StartUpApplication.SERVLET_PATH)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			return get(values);
		}
		
		@Override
		public ParameterURL get(Object... values)
		{
			var serverID = (int) values[0];
					
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			return url;
		}
	};
			
	public static final Endpoint GAME_TYPE_ADD = new Endpoint(GameTypeAdd.class, StartUpApplication.SERVLET_PATH)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ParameterURL get(Object... values)
		{	
			return getRequestURL();
		}
	};
	
	public static final Endpoint GAME_SERVER_DELETE = new Endpoint(GameServerDelete.class, StartUpApplication.SERVLET_PATH)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ParameterURL get(Object... values)
		{
			var serverID = (int) values[0];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			
			return url;
		}
	};
			
	public static final Endpoint GAME_SERVER_FILE_DELETE = new Endpoint(GameServerFileDelete.class, StartUpApplication.SERVLET_PATH)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
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
	};
	
	public static final Endpoint GAME_SERVER_FILE_DELETE_MULTIPLE = new Endpoint(GameServerFileDeleteMultiple.class, StartUpApplication.SERVLET_PATH)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
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
	};
	
	public static final Endpoint GAME_SERVER_FILE_DOWNLOAD = new Endpoint(GameServerFileDownload.class, StartUpApplication.SERVLET_PATH)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			return get(values);
		}
		
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
	};
	
	public static final Endpoint GAME_SERVER_FILE_RENAME = new Endpoint(GameServerFileRename.class, StartUpApplication.SERVLET_PATH)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public ParameterURL get(Object... values)
		{
			var serverID = (int) values[0];
			var directories = (List<String>) values[1];
			var rename = (String) values[2];
			var newFolder = (boolean) values[3];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
			
			if(rename != null && !rename.isEmpty())
			{
				url.addQuery(newFolder ? ApiSettings.NEW_FOLDER.getName() : ApiSettings.RENAME.getName(), rename);
			}
			
			return url;
		}
	};
	
	public static final Endpoint GAME_SERVER_TRIGGER_ADD = new Endpoint(GameServerTriggerAdd.class, StartUpApplication.SERVLET_PATH)
	{
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
		
		@Override
		public ParameterURL get(Object... values)
		{
			throw new UnsupportedOperationException();
		}
	};
			
	public static final Endpoint GAME_SERVER_TRIGGER_DELETE = new Endpoint(GameServerTriggerDelete.class, StartUpApplication.SERVLET_PATH)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
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
	};
	
	public static final Endpoint GAME_SERVER_TRIGGER_EDIT = new Endpoint(GameServerTriggerEdit.class, StartUpApplication.SERVLET_PATH)
	{
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
		
		@Override
		public ParameterURL get(Object... values)
		{
			throw new UnsupportedOperationException();
		}
	};
	
	public static final Endpoint SERVER_INTERACT = new Endpoint(ServerInteract.class, StartUpApplication.SERVLET_PATH)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			return get(values);
		}
		
		@Override
		public ParameterURL get(Object... values)
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
	};
}
