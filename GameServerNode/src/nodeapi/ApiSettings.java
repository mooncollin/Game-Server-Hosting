package nodeapi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import utils.Utils;
import utils.functional.True;

public class ApiSettings <T>
{
	public static final int TOMCAT_HTTP_PORT = 8080;
	
	public static final Pattern SERVER_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_ ]+");
	
	public static final ApiSettings<String> SERVER_NAME = new ApiSettings<String>(String.class, "name", false, name -> SERVER_NAME_PATTERN.matcher(name).matches(), null);
	public static final ApiSettings<Integer> SERVER_ID = new ApiSettings<Integer>(Integer.class, "id", false);
	public static final ApiSettings<String> EXECUTABLE_NAME = new ApiSettings<String>(String.class, "execName", false, name -> !name.isBlank(), null);
	public static final ApiSettings<String> SERVER_TYPE = new ApiSettings<String>(String.class, "type", false);
	public static final ApiSettings<String> NODE_NAME = new ApiSettings<String>(String.class, "node", false);
	
	@SuppressWarnings("unchecked")
	public static final ApiSettings<List<String>> DIRECTORY = new ApiSettings<List<String>>((Class<List<String>>) Collections.<String>emptyList().getClass(), "directory", false, dir -> dir.stream()
																											       	  .allMatch(s -> !s.equals("..")
																											       		&& !((String) dir.get(0)).isBlank())
																											, (String dir) -> Optional.of(Arrays.asList(dir.split(","))));
	
	public static final ApiSettings<String> RENAME = new ApiSettings<String>(String.class, "rename", true);
	public static final ApiSettings<String> NEW_FOLDER = new ApiSettings<String>(String.class, "newFolder", true, folder -> folder == null || !folder.isBlank(), null);
	public static final ApiSettings<Boolean> FOLDER = new ApiSettings<Boolean>(Boolean.class, "folder", true, null, str -> Optional.of(Boolean.valueOf(str)));
	public static final ApiSettings<String[]> FILES = new ApiSettings<String[]>(String[].class, "files", false, files -> Arrays.stream(files)
																															   .allMatch(s -> !s.equals("..")
																														 && !files[0].isBlank())
																											  , files -> files == null ? Optional.empty() : Optional.of(files.split(",")));
	
	public static final ApiSettings<String> PROPERTY = new ApiSettings<String>(String.class, "property", false);
	public static final ApiSettings<String> COMMAND = new ApiSettings<String>(String.class, "command", false);
	
	public static final ApiSettings<Integer> TRIGGER_ID = new ApiSettings<Integer>(Integer.class, "triggerID", false);
	public static final ApiSettings<String> TRIGGER_VALUE = new ApiSettings<String>(String.class, "value", false);
	public static final ApiSettings<String> TRIGGER_COMMAND = new ApiSettings<String>(String.class, "command", true, null, command -> {
		if(command == null)
		{
			return Optional.of("");
		}
		
		return Optional.of(command);
	});
	public static final ApiSettings<String> TRIGGER_ACTION = new ApiSettings<String>(String.class, "action", true, null, action -> {
		if(action == null)
		{
			return Optional.of("");
		}
		
		return Optional.of(action);
	});
	public static final ApiSettings<String> TRIGGER_TYPE = new ApiSettings<String>(String.class, "type", false);
	public static final ApiSettings<String> OUTPUT_MODE = new ApiSettings<String>(String.class, "mode", true);
	
	public static final ApiSettings<Boolean> RESTARTS_UNEXPECTED = new ApiSettings<Boolean>(Boolean.class, "restartsUnexpected", true, null, str -> str == null ? Optional.of(false) : Optional.of(str.equals("on")));
	public static final ApiSettings<String> ARGUMENTS = new ApiSettings<String>(String.class, "arguments", false, null, str -> Optional.of(str.trim()));
	
	public static final ApiSettings<String> MODULE_NAME = new ApiSettings<String>(String.class, "modulename", false, null, null);
	public static final ApiSettings<String> MODULE_SERVLET_NAME = new ApiSettings<String>(String.class, "moduleservletname", false, null, null);
	
	private final String name;
	private final boolean nullable;
	private final Predicate<T> checker;
	private final Function<String, Optional<T>> parser;
	
	public ApiSettings(Class<T> clazz, String name, boolean nullable)
	{
		this(clazz, name, nullable, null, null);
	}
	
	public ApiSettings(Class<T> clazz, String name, boolean nullable, Predicate<T> checker)
	{
		this(clazz, name, nullable, checker, null);
	}
	
	public ApiSettings(Class<T> clazz, String name, boolean nullable, Function<String, Optional<T>> parser)
	{
		this(clazz, name, nullable, null, parser);
	}
	
	public ApiSettings(Class<T> clazz, String name, boolean nullable, Predicate<T> checker, Function<String, Optional<T>> parser)
	{
		this.name = Objects.requireNonNull(name);
		this.nullable = nullable;
		this.checker = Objects.requireNonNullElse(checker, new True<T>());
		this.parser = Objects.requireNonNullElse(parser, str -> Utils.fromString(clazz, str));
	}
	
	public boolean isNullable()
	{
		return nullable;
	}
	
	public String getName()
	{
		return name;
	}
	
	public Predicate<T> getChecker()
	{
		return checker;
	}
	
	public boolean valid(final T value)
	{
		if(!isNullable() && value == null)
		{
			return false;
		}

		return checker.test(value);
	}
	
	public Optional<T> parse(String str)
	{
		var<T> optionValue = parser.apply(str);
		if(optionValue.isEmpty() || !valid(optionValue.get()))
		{
			return Optional.empty();
		}
		
		return optionValue;
	}
	
	public Optional<T> parse(HttpServletRequest request)
	{
		return parse(request.getParameter(getName()));
	}
}
