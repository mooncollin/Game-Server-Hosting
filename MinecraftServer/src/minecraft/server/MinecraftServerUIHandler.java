package minecraft.server;

import java.util.Map;

import minecraft.frontend.Settings;
import nodeapi.ApiSettings;
import server.GameServerUIHandler;

public class MinecraftServerUIHandler extends GameServerUIHandler
{
	public static final ApiSettings<Integer> RAM_AMOUNT = new ApiSettings<Integer>(Integer.class, "ramAmount", 
				false, ram -> ram >= MinecraftServer.MINIMUM_HEAP_SIZE && ram % MinecraftServer.HEAP_STEP == 0, null);
	
	private static final Map<String, String> ICON_MAPPING = Map.ofEntries
	(
		Map.entry("Properties", "fas fa-wrench")
	);
	
	public MinecraftServerUIHandler()
	{
		registerServlet("Properties", new Settings());
	}
	
	@Override
	public String getIconClass(String servletName)
	{
		return ICON_MAPPING.get(servletName);
	}
}
