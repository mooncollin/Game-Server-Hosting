package backend.main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * A class that contains information from the controller properties files as well
 * as other application constants.
 * @author Collin
 *
 */
public class ControllerProperties
{
	/**
	 * The location of the controller properties file. This is taken from the system's
	 * environmental variables.
	 */
	public static final String PROPERTIES_LOCATION = System.getenv("GAME_SERVER_CONTROLLER_PROPERTIES");
	
	/**
	 * The relative path to the Apache Velocity templates folder.
	 */
	public static final String TEMPLATES_PATH = "/templates";
	
	/**
	 * The list of node names directly taken from the properties file.
	 */
	public static final String NODE_NAMES;
	
	/**
	 * The list of node ip addresses directly taken from the properties file.
	 */
	public static final String NODE_ADDRESSES;
	
	/**
	 * The url for the database taken from the properties file.
	 */
	public static final String DATABASE_URL;
	
	/**
	 * The username for the database taken from the properties file.
	 */
	public static final String DATABASE_USERNAME;
	
	/**
	 * The password for the database taken from the properties file.
	 */
	public static final String DATABASE_PASSWORD;
	
	/**
	 * The object holding the properties from the properties file.
	 */
	private static final Properties properties;
	
	static
	{
		properties = new Properties();
		FileReader fileIn;
		try
		{
			fileIn = new FileReader(PROPERTIES_LOCATION);
		} catch (FileNotFoundException e)
		{
			throw new RuntimeException("Cannot find Game Server Controller properties file");
		}
		
		try
		{
			properties.load(fileIn);
			fileIn.close();
		} catch (IOException e)
		{
			throw new RuntimeException("Error reading Game Server Controller properties file");
		}
		
		NODE_NAMES = properties.getProperty("node_names");
		NODE_ADDRESSES = properties.getProperty("node_addresses");
		DATABASE_URL = properties.getProperty("database_url");
		DATABASE_USERNAME = properties.getProperty("database_username");
		DATABASE_PASSWORD = properties.getProperty("database_password");
	}
	
	/**
	 * Returns the current properties.
	 * @return controller properties
	 */
	public static Properties getProperties()
	{
		return properties;
	}
}
