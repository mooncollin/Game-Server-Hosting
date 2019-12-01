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
		
		nodeUsageAddresses.addElements(
			StartUpApplication.nodeAddresses
							  .values()
							  .stream()
							  .map(address -> String.format("ws://%s/NodeUsage", address))
							  .toArray(String[]::new)
		);
		
		return nodeUsageAddresses.toString();
	}
	
	public static JavascriptMap<Integer, String> getServerStartInteractAddresses()
	{
		var startServerAddressMap = new JavascriptMap<Integer, String>("startServerAddresses");
		
		for(var serverID : StartUpApplication.serverAddresses.keySet())
		{
			startServerAddressMap.set(serverID, ServerInteract.getEndpoint(serverID, "start"));
		}
		
		return startServerAddressMap;
	}
	
	public static JavascriptMap<Integer, String> getServerStopInteractAddresses()
	{
		var stopServerAddressMap = new JavascriptMap<Integer, String>("stopServerAddresses");
		
		for(var serverID : StartUpApplication.serverAddresses.keySet())
		{
			stopServerAddressMap.set(serverID, ServerInteract.getEndpoint(serverID, "stop"));
		}
		
		return stopServerAddressMap;
	}
	
	public static JavascriptMap<Integer, String> getNodeOutputAddresses()
	{
		var nodeOutputAddresses = new JavascriptMap<Integer, String>("nodeOutputAddresses");
		
		for(var entry : StartUpApplication.serverAddresses.entrySet())
		{
			var serverID = entry.getKey();
			var nodeAddress = entry.getValue();
			
			nodeOutputAddresses.set(serverID, String.format("ws://%s%s", nodeAddress, api.Output.getEndpoint(serverID, "running")));
		}
		
		return nodeOutputAddresses;
	}
	
	public static JavascriptVariable<String> createSocketAddress(String name, String serverAddress, int serverID)
	{
		return new JavascriptVariable<String>(name, String.format("%s%s%s", api.Output.PROTOCOL, serverAddress, api.Output.getEndpoint(serverID)));
	}
	
	public static JavascriptVariable<String> createInteractAddress(String name, int serverID, String command)
	{
		return new JavascriptVariable<String>(name, ServerInteract.getEndpoint(serverID, command));
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
