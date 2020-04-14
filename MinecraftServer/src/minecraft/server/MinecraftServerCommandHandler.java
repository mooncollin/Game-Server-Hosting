package minecraft.server;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.simple.JSONObject;
import server.Command;
import server.GameServerCommandHandler;
import utils.servlet.HttpStatus;

/**
 * A command handler for generic game servers.
 * Implements the following commands:
 * <table>
 * 	<thead>
 * 		<th>Command</th>
 * 		<th>What it does</th>
 * 		<th>Status Code</th>
 * 		<th>Request</th>
 * 		<th>Response</th>
 * 	</thead>
 * 	<tbody>
 * 		<tr>
 * 			<td>getProperties</td>
 * 			<td>Gets the server properties from the properties file</td>
 * 			<td>200</td>
 * 			<td><code>{}</td></code>
 * 			<td><code>{'result': object}</code></td>
 * 		</tr>
 *  	<tr>
 * 			<td>setProperties</td>
 * 			<td>Sets the server properties</td>
 * 			<td>200 if incoming request is valid, 400 otherwise</td>
 * 			<td><code>{'value': object}</code></td>
 * 			<td><code>{}</code></td>
 * 		</tr>
 * 	</tbody>
 * </table>
 * @author Collin
 *
 *
 */
public class MinecraftServerCommandHandler extends GameServerCommandHandler<MinecraftServer>
{
	public static final String GET_PROPERTIES_COMMAND_NAME = "getProperties";
	public static final String SET_PROPERTIES_COMMAND_NAME = "setProperties";
	
	private final Command getPropertiesCommand;
	private final Command setPropertiesCommand;
	
	public MinecraftServerCommandHandler()
	{
		this(null);
	}
	
	public MinecraftServerCommandHandler(MinecraftServer server)
	{
		super(server);
		
		getPropertiesCommand = new Command(GET_PROPERTIES_COMMAND_NAME)
		{
			@SuppressWarnings("unchecked")
			@Override
			public HttpStatus apply(JSONObject request, JSONObject response)
			{
				response.put("result", server.getProperties());
				return HttpStatus.OK;
			}
		};
		
		setPropertiesCommand = new Command(SET_PROPERTIES_COMMAND_NAME)
		{
			@Override
			public HttpStatus apply(JSONObject request, JSONObject response)
			{
				try
				{
					var jsonProperties = (JSONObject) request.get("value");
					var properties = new Properties();
					for(var entry : MinecraftServer.MINECRAFT_PROPERTIES.entrySet())
					{
						properties.setProperty(entry.getKey(), entry.getValue().toString());
					}
					for(var key : jsonProperties.keySet())
					{
						properties.setProperty(key.toString(), jsonProperties.get(key).toString());
					}
					
					server.setProperties(properties);
					return HttpStatus.OK;
				}
				catch (ClassCastException e)
				{
					return HttpStatus.BAD_REQUEST;
				}
			}
		};
	}
	
	@Override
	public List<Command> getCommands()
	{
		var originalCommands = super.getCommands();
		
		var myCommands = List.of(
			getPropertiesCommand,
			setPropertiesCommand
		);
		
		return Stream.concat(originalCommands.stream(), myCommands.stream())
				  	 .collect(Collectors.toList());
	}
}