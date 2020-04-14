package frontend.templates;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.velocity.shaded.commons.io.FilenameUtils;

import frontend.Endpoints;
import model.Table;
import models.TriggersTable;
import server.GameServerModule;
import server.TriggerHandler;
import server.TriggerHandlerRecurring;
import server.TriggerHandlerTime;

public class Templates
{
	/**
	 * A class for holding information about a server to be used in templates.
	 * @author Collin
	 *
	 */
	public static class ServerInfo
	{
		private int id;
		private String name;
		private String serverType;
		private GameServerModule module;
		
		/**
		 * Constructor.
		 * @param id id of a server
		 * @param name the name of a server
		 * @param serverType the type of a server
		 * @param module the module associated with the given server type
		 */
		public ServerInfo(int id, String name, String serverType, GameServerModule module)
		{
			this.id = id;
			this.name = name;
			this.serverType = serverType;
			this.module = module;
		}
		
		/**
		 * Gets the name of the server
		 * @return name of the server
		 */
		public String getName()
		{
			return name;
		}
		
		/**
		 * Gets the id of the server
		 * @return server id
		 */
		public int getId()
		{
			return id;
		}
		
		/**
		 * Gets the type of the server
		 * @return server type
		 */
		public String getServerType()
		{
			return serverType;
		}
		
		/**
		 * Gets the module implementation that creates these types of servers
		 * @return server module
		 */
		public GameServerModule getModule()
		{
			return module;
		}
	}
	
	/**
	 * A class for holding trigger information used in templates.
	 * @author Collin
	 *
	 */
	public static class TriggerInfo
	{
		private int id;
		private String type;
		private String value;
		private String command;
		private String extra;
		
		/**
		 * Constructor.
		 * @param id id of a trigger
		 * @param type type of a trigger
		 * @param value value of a trigger
		 * @param command command of a trigger
		 * @param extra action of a trigger
		 */
		public TriggerInfo(int id, String type, String value, String command, String extra)
		{
			this.id = id;
			this.type = type;
			this.value = value;
			this.command = command;
			this.extra = extra;
			
			if(type.equals(TriggerHandler.RECURRING_TYPE))
			{
				this.value = TriggerHandlerRecurring.convertSecondsToFormat(Long.valueOf(this.value));
			}
			else if(type.equals(TriggerHandler.TIME_TYPE))
			{
				this.value = TriggerHandlerTime.convertSecondsToFormat(Long.valueOf(this.value));
			}
		}
		
		/**
		 * Constructor. Creates from a table.
		 * @param table a database row
		 */
		public TriggerInfo(Table table)
		{
			this(table.getColumnValue(TriggersTable.ID),
				 table.getColumnValue(TriggersTable.TYPE),
				 table.getColumnValue(TriggersTable.VALUE),
				 table.getColumnValue(TriggersTable.COMMAND),
				 table.getColumnValue(TriggersTable.EXTRA));
		}
		
		/**
		 * Gets the trigger id.
		 * @return trigger id
		 */
		public int getId()
		{
			return id;
		}
		
		/**
		 * Gets the trigger type.
		 * @return trigger type
		 */
		public String getType()
		{
			return type;
		}
		
		/**
		 * Gets the trigger's value.
		 * @return trigger value
		 */
		public String getValue()
		{
			return value;
		}
		
		/**
		 * Gets the trigger's action.
		 * @return trigger action
		 */
		public String getExtra()
		{
			return extra;
		}
		
		/**
		 * Gets the command of the trigger
		 * @return trigger command
		 */
		public String getCommand()
		{
			return command;
		}
	}
	
	/**
	 * A class used for containing file information using in templates.
	 * @author Collin
	 *
	 */
	public static class FileInfo
	{
		/**
		 * A mapping from file types to Font Awesome icon names.
		 */
		private static final Map<String, String> FILE_TYPES_FONT_AWESOME = Map.ofEntries
		(
				Map.entry("zip", "archive"),
				Map.entry("tar", "archive"),
				Map.entry("gz", "archive"),
				Map.entry("rar", "archive"),
				Map.entry("jar", "archive"),
				Map.entry("mp3", "audio"),
				Map.entry("ogg", "audio"),
				Map.entry("wav", "audio"),
				Map.entry("py", "code"),
				Map.entry("java", "code"),
				Map.entry("c", "code"),
				Map.entry("cpp", "code"),
				Map.entry("lua", "code"),
				Map.entry("pl", "code"),
				Map.entry("html", "code"),
				Map.entry("css", "code"),
				Map.entry("js", "code"),
				Map.entry("xml", "code"),
				Map.entry("md", "code"),
				Map.entry("json", "code"),
				Map.entry("jpg", "image"),
				Map.entry("jpeg", "image"),
				Map.entry("png", "image"),
				Map.entry("svg", "image"),
				Map.entry("mp4", "movie"),
				Map.entry("ppt", "powerpoint"),
				Map.entry("pptx", "powerpoint"),
				Map.entry("doc", "word"),
				Map.entry("docx", "word"),
				Map.entry("txt", "alt"),
				Map.entry("properties", "alt"),
				Map.entry("log", "alt")
		);
		
		private String name;
		private boolean directory;
		private String icon;
		
		/**
		 * Constructor.
		 * @param name name of the file
		 * @param directory true if this is a directory, false otherwise
		 */
		public FileInfo(String name, boolean directory)
		{
			this.name = name;
			this.directory = directory;
			this.icon = FILE_TYPES_FONT_AWESOME.getOrDefault(FilenameUtils.getExtension(name), "file");
		}
		
		/**
		 * The name of the file.
		 * @return name of the file
		 */
		public String getName()
		{
			return name;
		}
		
		/**
		 * Whether this file is a directory.
		 * @return true if it is a directory, false otherwise
		 */
		public boolean isDirectory()
		{
			return directory;
		}
		
		/**
		 * Gets the icon name associated with this file.
		 * @return icon type
		 */
		public String getIcon()
		{
			return icon;
		}
	}
	
	/**
	 * A class for containing directory information used in templates.
	 * @author Collin
	 *
	 */
	public static class DirectoryEntry
	{
		private String name;
		private List<String> path;
		
		/**
		 * Constructor.
		 * @param name name of the file
		 * @param path path information leading from the file
		 */
		public DirectoryEntry(String name, List<String> path)
		{
			this.name = name;
			this.path = List.copyOf(path);
		}
		
		/**
		 * Gets the name of the file.
		 * @return file name
		 */
		public String getName()
		{
			return name;
		}
		
		/**
		 * Gets the path of the file, excluding the file name.
		 * @return file path
		 */
		public List<String> getPath()
		{
			return path;
		}
		
		/**
		 * Gets the path of the file, including the file name.
		 * @return file path
		 */
		public List<String> getFullPath()
		{
			var full = new LinkedList<String>(path);
			full.add(name);
			
			return full;
		}
	}
	
	/**
	 * A class for containing information about nodes for use in templates.
	 * @author Collin
	 *
	 */
	public static class NodeInfo
	{
		private String name;
		
		/**
		 * Constructor.
		 * @param name name of a node
		 */
		public NodeInfo(String name)
		{
			this.name = name;
		}
		
		/**
		 * Gets the name of this node.
		 * @return node name
		 */
		public String getName()
		{
			return name;
		}
	}
	
	/**
	 * Gets the endpoint as a string of server commands.
	 * @return url string
	 */
	public static String getServerCommandEndpoint()
	{
		return Endpoints.SERVER_INTERACT.post().getURL();
	}
	
	/**
	 * A class for containing information about modules for use in templates.
	 * @author Collin
	 *
	 */
	public static class ModuleInfo
	{
		private String name;
		private String icon;
		
		/**
		 * Constructor.
		 * @param name name of the module
		 * @param icon a icon of the module
		 */
		public ModuleInfo(String name, String icon)
		{
			this.name = name;
			this.icon = icon;
		}
		
		/**
		 * Gets the name of this module.
		 * @return module name
		 */
		public String getName()
		{
			return name;
		}
		
		/**
		 * Gets the icon associated with this module.
		 * @return module icon
		 */
		public String getIcon()
		{
			return icon;
		}
	}
}
