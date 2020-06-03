package nodeapi;

import java.util.List;

import nodemain.StartUpApplication;
import utils.servlet.HttpEndpoint;
import utils.servlet.ParameterURL;
import utils.servlet.WebSocketEndpoint;

public class Endpoints
{
	private Endpoints() {}
	
	public static final WebSocketEndpoint NODE_USAGE = new WebSocketEndpoint(NodeUsage.class, "NodeUsage", StartUpApplication.APPLICATION_PORT)
	{
		public ParameterURL open(Object... values)
		{
			return getRequestURL();
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final WebSocketEndpoint OUTPUT = new WebSocketEndpoint(Output.class, "Output", StartUpApplication.APPLICATION_PORT)
	{
		public ParameterURL open(Object... values)
		{
			var url = getRequestURL();
			
			var serverID = (int) values[0];
			if(values.length > 1)
			{
				var mode = (String) values[1];
				url.addQuery(ApiSettings.OUTPUT_MODE.getName(), mode);
			}
			
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			return url;
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint FILE_DELETE = new HttpEndpoint(FileDelete.class, StartUpApplication.APPLICATION_PORT)
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
			var directories = (List<String>) values[0];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
			return url;
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint FILE_DOWNLOAD = new HttpEndpoint(FileDownload.class, StartUpApplication.APPLICATION_PORT)
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
			var directories = (List<String>) values[0];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
			return url;
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint FILE_RENAME = new HttpEndpoint(FileRename.class, StartUpApplication.APPLICATION_PORT)
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
			var directories = (List<String>) values[0];
			var rename = (String) values[1];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
			url.addQuery(ApiSettings.RENAME.getName(), rename);
			return url;
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint NEW_FOLDER = new HttpEndpoint(NewFolder.class, StartUpApplication.APPLICATION_PORT)
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
			var directories = (List<String>) values[0];
			var newFolder = (String) values[1];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
			url.addQuery(ApiSettings.NEW_FOLDER.getName(), newFolder);
			return url;
			
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint FILE_UPLOAD = new HttpEndpoint(FileUpload.class, StartUpApplication.APPLICATION_PORT)
	{
		@SuppressWarnings("unchecked")
		@Override
		public ParameterURL post(Object... values)
		{
			var directories = (List<String>) values[0];
			var isFolder = (boolean) values[1];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
			url.addQuery(ApiSettings.FOLDER.getName(), isFolder);
			return url;
		}
		
		@Override
		public ParameterURL get(Object... values)
		{
			throw new UnsupportedOperationException();
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint SERVER_ADD = new HttpEndpoint(ServerAdd.class, StartUpApplication.APPLICATION_PORT)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			var serverID = (int) values[0];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			return url;
		}
		
		@Override
		public ParameterURL get(Object... values)
		{
			throw new UnsupportedOperationException();
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint SERVER_DELETE = new HttpEndpoint(ServerDelete.class, StartUpApplication.APPLICATION_PORT)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			var serverID = (int) values[0];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			return url;
		}
		
		@Override
		public ParameterURL get(Object... values)
		{
			throw new UnsupportedOperationException();
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint SERVER_EDIT = new HttpEndpoint(ServerEdit.class, StartUpApplication.APPLICATION_PORT)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			var serverID = (int) values[0];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			return url;
		}
		
		@Override
		public ParameterURL get(Object... values)
		{
			throw new UnsupportedOperationException();
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint SERVER_FILES = new HttpEndpoint(ServerFiles.class, StartUpApplication.APPLICATION_PORT)
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
			var directories = (List<String>) values[0];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.DIRECTORY.getName(), String.join(",", directories));
			return url;
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint SERVER_INTERACT = new HttpEndpoint(ServerInteract.class, StartUpApplication.APPLICATION_PORT)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			var serverID = (int) values[0];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.SERVER_ID.getName(), serverID);
			return url;
		}
		
		@Override
		public ParameterURL get(Object... values)
		{
			throw new UnsupportedOperationException();
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint TRIGGER_DELETE = new HttpEndpoint(TriggerDelete.class, StartUpApplication.APPLICATION_PORT)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ParameterURL get(Object... values)
		{
			var triggerID = (int) values[0];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.TRIGGER_ID.getName(), triggerID);
			return url;
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint TRIGGER_EDIT = new HttpEndpoint(TriggerEdit.class, StartUpApplication.APPLICATION_PORT)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ParameterURL get(Object... values)
		{
			var triggerID = (int) values[0];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.TRIGGER_ID.getName(), triggerID);
			return url;
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
	
	public static final HttpEndpoint MODULE_UPDATE = new HttpEndpoint(UpdateModule.class, StartUpApplication.APPLICATION_PORT)
	{
		@Override
		public ParameterURL post(Object... values)
		{
			var moduleName = (String) values[0];
			
			var url = getRequestURL();
			url.addQuery(ApiSettings.MODULE_NAME.getName(), moduleName);
			return url;
		}
		
		@Override
		public ParameterURL get(Object... values)
		{
			throw new UnsupportedOperationException();
		}
	}.prefix(StartUpApplication.SERVLET_ROOT);
}
