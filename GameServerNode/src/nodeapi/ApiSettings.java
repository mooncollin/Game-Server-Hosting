package nodeapi;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import utils.servlet.HttpParameter;
import utils.servlet.HttpParameterBuilder;
import utils.types.ClassUtils;

/**
 * Class for holding constants for managing http parameters.
 * @author Collin
 *
 */
public class ApiSettings
{
	private ApiSettings() {}
	
	public static final int TOMCAT_HTTP_PORT = 8080;
	
	public static final Pattern SERVER_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_ ]+");
	
	public static final HttpParameter<String> SERVER_NAME = HttpParameterBuilder.start(String.class)
																				.setName("name")
																				.setChecker(name -> SERVER_NAME_PATTERN.matcher(name).matches())
																				.build();
	
	public static final HttpParameter<Integer> SERVER_ID = HttpParameterBuilder.start(Integer.class)
																			   .setName("id")
																			   .build();
	
	public static final HttpParameter<String> EXECUTABLE_NAME = HttpParameterBuilder.start(String.class)
																					.setName("execName")
																					.setChecker(name -> !name.isBlank())
																					.build();
	
	public static final HttpParameter<String> SERVER_TYPE = HttpParameterBuilder.start(String.class)
																				.setName("type")
																				.build();
	
	public static final HttpParameter<String> NODE_NAME = HttpParameterBuilder.start(String.class)
																			  .setName("node")
																			  .build();
	
	public static final HttpParameter<List<String>> DIRECTORY = HttpParameterBuilder.start(ClassUtils.<String>listClass())
																					.setName("directory")
																					.setChecker(dir -> dir.stream().allMatch(s -> !s.equals("..") && !dir.get(0).isBlank()))
																					.setParser(dir -> Optional.of(Arrays.asList(dir.split(","))))
																					.build();
	
	public static final HttpParameter<String> RENAME = HttpParameterBuilder.start(String.class)
																		   .setName("rename")
																		   .isNullable(true)
																		   .build();
	
	public static final HttpParameter<String> NEW_FOLDER = HttpParameterBuilder.start(String.class)
																			   .setName("newFolder")
																			   .isNullable(true)
																			   .setChecker(folder -> folder == null || !folder.isBlank())
																			   .build();
	
	public static final HttpParameter<Boolean> FOLDER = HttpParameterBuilder.start(Boolean.class)
																			.setName("folder")
																			.isNullable(true)
																			.setParser(str -> Optional.of(Boolean.valueOf(str)))
																			.build();
	
	public static final HttpParameter<String[]> FILES = HttpParameterBuilder.start(String[].class)
																			.setName("files")
																			.setChecker(files -> Arrays.stream(files).allMatch(s -> !s.equals("..") && !files[0].isBlank()))
																			.setParser(files -> Optional.of(files.split(",")))
																			.build();
	
	public static final HttpParameter<String> PROPERTY = HttpParameterBuilder.start(String.class)
																			 .setName("property")
																			 .build();
	
	public static final HttpParameter<String> COMMAND = HttpParameterBuilder.start(String.class)
																			.setName("command")
																			.build();
	
	public static final HttpParameter<Integer> TRIGGER_ID = HttpParameterBuilder.start(Integer.class)
																				.setName("triggerID")
																				.build();
	
	public static final HttpParameter<String> TRIGGER_VALUE = HttpParameterBuilder.start(String.class)
																				  .setName("value")
																				  .build();
	
	public static final HttpParameter<String> TRIGGER_COMMAND = HttpParameterBuilder.start(String.class)
																					.setName("command")
																					.isNullable(true)
																					.setParser(command -> command == null ? Optional.of("") : Optional.of(command))
																					.build();
	
	public static final HttpParameter<String> TRIGGER_ACTION = HttpParameterBuilder.start(String.class)
																				   .setName("action")
																				   .isNullable(true)
																				   .setParser(action -> action == null ? Optional.of("") : Optional.of(action))
																				   .build();
	
	public static final HttpParameter<String> TRIGGER_TYPE = HttpParameterBuilder.start(String.class)
																				 .setName("type")
																				 .build();
	
	public static final HttpParameter<String> OUTPUT_MODE = HttpParameterBuilder.start(String.class)
																				.setName("mode")
																				.isNullable(true)
																				.build();
	
	public static final HttpParameter<Boolean> RESTARTS_UNEXPECTED = HttpParameterBuilder.start(Boolean.class)
																						 .setName("restartsUnexpected")
																						 .isNullable(true)
																						 .setParser(str -> str == null ? Optional.of(false) : Optional.of(str.equals("on")))
																						 .build();
	
	public static final HttpParameter<String> ARGUMENTS = HttpParameterBuilder.start(String.class)
																			  .setName("arguments")
																			  .setParser(str -> Optional.of(str.trim()))
																			  .build();
	
	public static final HttpParameter<String> MODULE_NAME = HttpParameterBuilder.start(String.class)
																				.setName("modulename")
																				.build();
	
	public static final HttpParameter<String> MODULE_SERVLET_NAME = HttpParameterBuilder.start(String.class)
																						.setName("moduleservletname")
																						.build();
}
