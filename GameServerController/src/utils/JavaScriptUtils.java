package utils;

import backend.main.ControllerProperties;
import backend.main.StartUpApplication;

public class JavaScriptUtils
{
//	public static String getServerNames()
//	{
//		final StringBuilder serverNames = new StringBuilder("var serverNames=[");
//		StartUpApplication.getServerInfo().keySet().forEach(name -> {
//			serverNames.append(String.format("'%s',", name));
//		});
//		serverNames.append("];");
//		return serverNames.toString();
//	}
	
	public static String getNodeNames()
	{
		StringBuilder nodeNames = new StringBuilder("var nodeNames=[");
		for(String name : ControllerProperties.NODE_NAMES.split(","))
		{
			nodeNames.append(String.format("'%s',", name));
		}
		nodeNames.append("];");
		
		return nodeNames.toString();
	}
	
	public static String getNodeAddresses(String prefix, String suffix)
	{
		final StringBuilder nodeAddresses = new StringBuilder("var nodeAddresses=[");
		StartUpApplication.getServerInfo().forEach((name, pair) -> {
			nodeAddresses.append(String.format("'%s%s%s',", prefix, pair.getSecond(), suffix));
		});
		nodeAddresses.append("];");
		return nodeAddresses.toString();
	}
	
	public static String getNodeUsageAddresses()
	{
		final StringBuilder nodeUsageAddresses = new StringBuilder("var nodeUsageAddresses=[");
		StartUpApplication.getServerInfo().forEach((name, pair) -> {
			nodeUsageAddresses.append(String.format("'ws://%s/NodeUsage',", pair.getSecond()));
		});
		nodeUsageAddresses.append("];");
		return nodeUsageAddresses.toString();
	}
}
