package backend.main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ControllerProperties
{
	public static final String PROPERTIES_LOCATION = System.getenv("GAME_SERVER_CONTROLLER_PROPERTIES");
	public static final String TEMPLATES_PATH = "/templates";
	public static final String NODE_NAMES;
	public static final String NODE_ADDRESSES;
	public static final String DATABASE_URL;
	public static final String DATABASE_USERNAME;
	public static final String DATABASE_PASSWORD;
	
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
	
	public static Properties getProperties()
	{
		return properties;
	}
}
