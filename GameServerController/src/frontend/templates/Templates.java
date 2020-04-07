package frontend.templates;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.velocity.shaded.commons.io.FilenameUtils;

import frontend.Endpoints;
import model.Table;
import models.TriggersTable;
import server.TriggerHandler;
import server.TriggerHandlerRecurring;
import server.TriggerHandlerTime;

public class Templates
{
	public static class ServerInfo
	{
		private int id;
		private String name;
		private String serverType;
		
		public ServerInfo(int id, String name, String serverType)
		{
			this.id = id;
			this.name = name;
			this.serverType = serverType;
		}
		
		public String getName()
		{
			return name;
		}
		
		public int getId()
		{
			return id;
		}
		
		public String getServerType()
		{
			return serverType;
		}
	}
	
	public static class TriggerInfo
	{
		private int id;
		private String type;
		private String value;
		private String command;
		private String extra;
		
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
		
		public TriggerInfo(Table table)
		{
			this(table.getColumnValue(TriggersTable.ID),
				 table.getColumnValue(TriggersTable.TYPE),
				 table.getColumnValue(TriggersTable.VALUE),
				 table.getColumnValue(TriggersTable.COMMAND),
				 table.getColumnValue(TriggersTable.EXTRA));
		}
		
		public int getId()
		{
			return id;
		}
		
		public String getType()
		{
			return type;
		}
		
		public String getValue()
		{
			return value;
		}
		
		public String getExtra()
		{
			return extra;
		}
		
		public String getCommand()
		{
			return command;
		}
	}
	
	public static class FileInfo
	{
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
		
		public FileInfo(String name, boolean directory)
		{
			this.name = name;
			this.directory = directory;
			this.icon = FILE_TYPES_FONT_AWESOME.getOrDefault(FilenameUtils.getExtension(name), "file");
		}
		
		public String getName()
		{
			return name;
		}
		
		public boolean isDirectory()
		{
			return directory;
		}
		
		public String getIcon()
		{
			return icon;
		}
	}
	
	public static class DirectoryEntry
	{
		private String name;
		private List<String> path;
		
		public DirectoryEntry(String name, List<String> path)
		{
			this.name = name;
			this.path = List.copyOf(path);
		}
		
		public String getName()
		{
			return name;
		}
		
		public List<String> getPath()
		{
			return path;
		}
		
		public List<String> getFullPath()
		{
			var full = new LinkedList<String>(path);
			full.add(name);
			
			return full;
		}
	}
	
	public static String minecraftPropertyToInputType(Object prop)
	{
		if(prop instanceof Integer)
		{
			return "number";
		}
		else if(prop instanceof Boolean)
		{
			return "checkbox";
		}
		
		return "text";
	}
	
	public static class PropertyInfo
	{
		private String name;
		private String type;
		private String value;
		
		public PropertyInfo(String name, String type, String value)
		{
			this.name = name;
			this.type = type;
			this.value = value;
		}
		
		public String getName()
		{
			return name;
		}
		
		public String getType()
		{
			return type;
		}
		
		public String getValue()
		{
			return value;
		}
	}
	
	public static class NodeInfo
	{
		private String name;
		private long totalRam;
		private long reservedRam;
		
		public NodeInfo(String name, long totalRam, long reservedRam)
		{
			this.name = name;
			this.totalRam = totalRam;
			this.reservedRam = reservedRam;
		}
		
		public String getName()
		{
			return name;
		}
		
		public long getTotalRam()
		{
			return totalRam;
		}
		
		public long getReservedRam()
		{
			return reservedRam;
		}
	}
	
	public static String getServerCommandEndpoint()
	{
		return Endpoints.SERVER_INTERACT.get().getURL();
	}
}
