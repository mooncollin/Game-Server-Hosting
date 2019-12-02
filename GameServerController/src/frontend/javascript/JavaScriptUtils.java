package frontend.javascript;

import backend.api.ServerInteract;
import backend.main.StartUpApplication;
import server.TriggerHandler;
import utils.StringUtils;

public class JavaScriptUtils
{	
	public static String getNodeNames()
	{
		var nodeNames = new JavascriptArray<String>("nodeNames");
		
		nodeNames.addElements(StartUpApplication.NODE_NAMES);
		
		return nodeNames.toString();
	}
	
	public static String getNodeUsageAddresses()
	{
		var nodeUsageAddresses = new JavascriptArray<String>("nodeUsageAddresses");
		
		for(var ipAddress : StartUpApplication.nodeIPAddresses.values())
		{
			var url = nodeapi.NodeUsage.getEndpoint();
			url.setHost(ipAddress);
			nodeUsageAddresses.add(url.getURL());
		}
		
		return nodeUsageAddresses.toString();
	}
	
	public static JavascriptMap<Integer, String> getServerStartInteractAddresses()
	{
		var startServerAddressMap = new JavascriptMap<Integer, String>("startServerAddresses");
		
		for(var serverID : StartUpApplication.serverIPAddresses.keySet())
		{
			startServerAddressMap.set(serverID, ServerInteract.getEndpoint(serverID, "start").getURL());
		}
		
		return startServerAddressMap;
	}
	
	public static JavascriptMap<Integer, String> getServerStopInteractAddresses()
	{
		var stopServerAddressMap = new JavascriptMap<Integer, String>("stopServerAddresses");
		
		for(var serverID : StartUpApplication.serverIPAddresses.keySet())
		{
			stopServerAddressMap.set(serverID, ServerInteract.getEndpoint(serverID, "stop").getURL());
		}
		
		return stopServerAddressMap;
	}
	
	public static JavascriptMap<Integer, String> getNodeOutputAddresses()
	{
		var nodeOutputAddresses = new JavascriptMap<Integer, String>("nodeOutputAddresses");
		
		for(var entry : StartUpApplication.serverIPAddresses.entrySet())
		{
			var serverID = entry.getKey();
			var nodeAddress = entry.getValue();
			var url = nodeapi.Output.getEndpoint(serverID, "running");
			url.setHost(nodeAddress);
			
			nodeOutputAddresses.set(serverID, url.getURL());
		}
		
		return nodeOutputAddresses;
	}
	
	public static JavascriptVariable<String> createSocketAddress(String name, String serverAddress, int serverID)
	{
		var url = nodeapi.Output.getEndpoint(serverID);
		url.setHost(serverAddress);
		return new JavascriptVariable<String>(name, url.getURL());
	}
	
	public static JavascriptVariable<String> createInteractAddress(String name, int serverID, String command)
	{
		return new JavascriptVariable<String>(name, ServerInteract.getEndpoint(serverID, command).getURL());
	}
	
	public static JavascriptArray<String> triggerTypes(String name)
	{
		return new JavascriptArray<String>(name, 
			StringUtils.capitalize(TriggerHandler.RECURRING_TYPE), 
			StringUtils.capitalize(TriggerHandler.TIME_TYPE),
			StringUtils.capitalize(TriggerHandler.OUTPUT_TYPE));
	}
	
	public static JavascriptArray<String> actionTypes(String name)
	{
		return new JavascriptArray<String>(name,
			TriggerHandler.START_SERVER,
			TriggerHandler.STOP_SERVER,
			TriggerHandler.RESTART_SERVER);
	}
}
