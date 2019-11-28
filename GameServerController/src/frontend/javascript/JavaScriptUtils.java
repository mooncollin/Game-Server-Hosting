package frontend.javascript;

import backend.api.ServerInteract;
import backend.main.StartUpApplication;

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
}
