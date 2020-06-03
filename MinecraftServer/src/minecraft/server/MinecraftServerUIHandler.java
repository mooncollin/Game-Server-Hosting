package minecraft.server;

import java.util.Map;

import minecraft.frontend.Settings;
import server.GameServerUIHandler;
import utils.servlet.HttpParameter;
import utils.servlet.HttpParameterBuilder;

public class MinecraftServerUIHandler extends GameServerUIHandler
{	
	public static final HttpParameter<Integer> RAM_AMOUNT = HttpParameterBuilder.start(Integer.class)
																				.setName("ramAmount")
																				.setChecker(ram -> ram >= MinecraftServer.MINIMUM_HEAP_SIZE && ram % MinecraftServer.HEAP_STEP == 0)
																				.build();
	
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
