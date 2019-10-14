package main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Objects;
import java.util.Properties;

import com.sun.management.OperatingSystemMXBean;

public class NodeProperties
{
	public static final String PROPERTIES_LOCATION;
	public static final String NAME;
	public static final String DEPLOY_FOLDER;
	public static final int MAX_RAM;
	public static final String DATABASE_URL;
	public static final String DATABASE_USERNAME;
	public static final String DATABASE_PASSWORD;
	public static final String JAVA8;
	private static final Properties properties;
	public static final long BYTES_IN_MEGABYTE = 1048576;
	
	static
	{
		PROPERTIES_LOCATION = System.getenv("GAME_SERVER_NODE_PROPERTIES");
		JAVA8 = Objects.requireNonNull(System.getenv("JAVA8"), "Do you have the JAVA8 environment variable set?");
		properties = new Properties();
		FileReader fileIn;
		try
		{
			fileIn = new FileReader(PROPERTIES_LOCATION);
		} catch (FileNotFoundException e)
		{
			throw new RuntimeException("Cannot find Game Server Node properties file");
		}
		
		try
		{
			properties.load(fileIn);
			fileIn.close();
		} catch (IOException e)
		{
			throw new RuntimeException("Error reading Game Server Node properties file");
		}
		
		NAME = properties.getProperty("name");
		DEPLOY_FOLDER = properties.getProperty("deploy_folder");
		DATABASE_URL = properties.getProperty("database_url");
		DATABASE_USERNAME = properties.getProperty("database_username");
		DATABASE_PASSWORD = properties.getProperty("database_password");
		String ramStr = properties.getProperty("max_ram");
		if(ramStr == null)
		{
			ramStr = "-1";
		}
		int ramAmount = 0;
		try
		{
			ramAmount = Integer.valueOf(ramStr);
		}
		catch(NumberFormatException e)
		{
		}
		
		if(ramAmount < 0)
		{
			OperatingSystemMXBean system = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
			long totalRamMB = system.getTotalPhysicalMemorySize() / NodeProperties.BYTES_IN_MEGABYTE;
			ramAmount = (int) totalRamMB;
		}
		
		MAX_RAM = ramAmount;
		if(MAX_RAM == 0)
		{
			throw new RuntimeException("Invalid RAM property");
		}
	}
	
	public static Properties getProperties()
	{
		return properties;
	}
}
