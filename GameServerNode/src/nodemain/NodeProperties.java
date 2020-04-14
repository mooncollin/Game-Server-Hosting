package nodemain;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class NodeProperties
{
	public static final String PROPERTIES_LOCATION;
	public static final String NAME;
	public static final String DEPLOY_FOLDER;
	public static final String DATABASE_URL;
	public static final String DATABASE_USERNAME;
	public static final String DATABASE_PASSWORD;
	private static final Properties properties;
	
	static
	{
		PROPERTIES_LOCATION = Objects.requireNonNull(System.getenv("GAME_SERVER_NODE_PROPERTIES"), "Do you have the GAME_SERVER_NODE_PROPERTIES environment variable set?");
		properties = new Properties();
		
		try(var fileIn = new FileReader(PROPERTIES_LOCATION))
		{
			properties.load(fileIn);
		} catch (FileNotFoundException e1)
		{
			throw new RuntimeException("Cannot find Game Server Node properties file");
		} catch (IOException e1)
		{
			throw new RuntimeException("Error reading Game Server Node properties file");
		}
		
		NAME = properties.getProperty("name");
		DEPLOY_FOLDER = properties.getProperty("deploy_folder");
		DATABASE_URL = properties.getProperty("database_url");
		DATABASE_USERNAME = properties.getProperty("database_username");
		DATABASE_PASSWORD = properties.getProperty("database_password");
	}
	
	public static Properties getProperties()
	{
		return properties;
	}
}
