package server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

import nodemain.StartUpApplication;

public class GameServerModuleLoader
{
	public static GameServerModule loadModule(InputStream stream) throws IOException
	{
		Path file = null;
		try
		{
			file = Files.createTempFile("gameservermodule", ".jar");
			try(var fileOut = new FileOutputStream(file.toFile()))
			{
				stream.transferTo(fileOut);
			}
			
			return loadModule(file.toFile());
		}
		finally
		{
			if(file != null)
			{
				file.toFile().delete();
			}
		}
	}
	
	public static GameServerModule loadModule(byte[] raw) throws IOException
	{
		Path file = null;
		try
		{
			file = Files.createTempFile("gameservermodule", ".jar");
			try(var fileOut = new FileOutputStream(file.toFile()))
			{
				fileOut.write(raw);
			}
			
			return loadModule(file.toFile());
		}
		finally
		{
			if(file != null)
			{
				file.toFile().delete();
			}
		}
	}
	
	public static GameServerModule loadModule(File location) throws IOException
	{
		URLClassLoader cl;
		try
		{
			cl = URLClassLoader.newInstance(new URL[] {location.toURI().toURL()}, StartUpApplication.class.getClassLoader());
		} catch (MalformedURLException e)
		{
			return null;
		}
		
		try(var jar = new JarFile(location))
		{
			var entries = jar.entries();
			while(entries.hasMoreElements())
			{
				var entry = entries.nextElement();
				if(!entry.isDirectory() && 
					entry.getName().contains(".class"))
				{
					var className = classFileToClassName(entry.getName());
					try
					{
						var clazz = cl.loadClass(className);
						if(GameServerModule.class.isAssignableFrom(clazz) &&
						   !clazz.equals(GameServerModule.class))
						{
							return clazz.asSubclass(GameServerModule.class).getConstructor().newInstance();
						}
					} catch (NoClassDefFoundError | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e)
					{
					}
				}
			}
		}
		
		return null;
	}
	
	public static void loadModuleResources(byte[] raw, File outDir) throws IOException
	{
		Path file = null;
		try
		{
			file = Files.createTempFile("gameservermodule", ".jar");
			try(var fileOut = new FileOutputStream(file.toFile()))
			{
				fileOut.write(raw);
			}
			
			loadModuleResources(file.toFile(), outDir);
		}
		finally
		{
			if(file != null)
			{
				file.toFile().delete();
			}
		}
	}
	
	public static void loadModuleResources(File location, File outDir) throws IOException
	{
		URLClassLoader cl;
		try
		{
			cl = URLClassLoader.newInstance(new URL[] {location.toURI().toURL()}, StartUpApplication.class.getClassLoader());
		} catch (MalformedURLException e)
		{
			return;
		}
		
		String moduleName = null;
		
		try(var jar = new JarFile(location))
		{
			var entries = jar.entries();
			while(entries.hasMoreElements())
			{
				var entry = entries.nextElement();
				if(!entry.isDirectory() && 
					entry.getName().contains(".class"))
				{
					var className = classFileToClassName(entry.getName());
					try
					{
						var clazz = cl.loadClass(className);
						if(GameServerModule.class.isAssignableFrom(clazz) &&
						   !clazz.equals(GameServerModule.class))
						{
							var module = clazz.asSubclass(GameServerModule.class).getConstructor().newInstance();
							moduleName = module.gameServerOptions().getServerType();
							break;
						}
					} catch (NoClassDefFoundError | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e)
					{
					}
				}
			}
		}
		
		if(moduleName != null)
		{
			try(var jar = new JarFile(location))
			{
				var entries = jar.entries();
				while(entries.hasMoreElements())
				{
					var entry = entries.nextElement();
					var path = Path.of(entry.getName());
					if(path.startsWith("resources"))
					{
						var modifiedPath = path.subpath(1, path.getNameCount());
						var parentFolder = modifiedPath.getParent();
						if(parentFolder == null)
						{
							parentFolder = Path.of("");
						}
						var fileName = path.getName(path.getNameCount()-1);
						var realPath = outDir.toPath().resolve(parentFolder).resolve(moduleName).resolve(fileName);
						realPath.toFile().getParentFile().mkdirs();
						try(var fileOut = new FileOutputStream(realPath.toFile()))
						{
							cl.getResourceAsStream(entry.getName()).transferTo(fileOut);
						}
					}
				}
			}
		}
	}
	
	public static String classFileToClassName(String file)
	{
		var baseFile = file.substring(0, file.length() - ".class".length());
		baseFile = baseFile.replace('/', '.');
		return baseFile;
	}
}
