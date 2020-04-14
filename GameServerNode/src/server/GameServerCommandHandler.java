package server;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.json.simple.JSONObject;

import nodemain.StartUpApplication;
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
 * 			<td>start</td>
 * 			<td>Starts the server</td>
 * 			<td>200 if server starts successfully, 500 otherwise</td>
 *  		<td><code>{}</code></td>
 * 			<td><code>{}</code></td>
 * 		</tr>
 *  	<tr>
 * 			<td>stop</td>
 * 			<td>Stops the server</td>
 * 			<td>200 if server stops successfully, 500 otherwise</td>
 * 			<td><code>{}</code></td>
 * 			<td><code>{}</code></td>
 * 		</tr>
 *  	<tr>
 * 			<td>restart</td>
 * 			<td>Restarts the server</td>
 * 			<td>200 if server restarts successfully, 500 otherwise</td>
 * 			<td><code>{}</code></td>
 * 			<td><code>{}</code></td>
 * 		</tr>
 *  	<tr>
 * 			<td>log</td>
 * 			<td>Gets a log of the recent server output</td>
 * 			<td>200</td>
 * 			<td><code>{}</code></td>
 * 			<td><code>{'result': string}</code></td>
 * 		</tr>
 * 		<tr>
 * 			<td>running</td>
 * 			<td>Checks if the server is running</td>
 * 			<td>200</td>
 * 			<td><code>{}</code></td>
 * 			<td><code>{'result': boolean}</code></td>
 * 		</tr>
 * 		<tr>
 * 			<td>ipaddress</td>
 * 			<td>Gets the full IP address that is required to connect to the server from anywhere</td>
 * 			<td>200</td>
 * 			<td><code>{}</code></td>
 * 			<td><code>{'result': string}</code></td>
 * 		</tr>
 * 		<tr>
 * 			<td>input</td>
 * 			<td>Delivers input to the server. Whatever is in the body part of the request gets transfered to the server.</td>
 * 			<td>
 * 				<p>200 if successfully wrote to the server.</p>
 * 				<p>400 if the server was not running beforehand.</p>
 * 				<p>500 if there was an error writing to the server.</p>
 * 			</td>
 * 			<td><code>{'value': string}</code></td>
 * 			<td><code>{}</code></td>
 * 		</tr>
 * 		<tr>
 * 			<td>autorestarts</td>
 * 			<td>Gets the auto restart feature of the server. Either true or false if enabled/disabled, or null if it does not support it.</td>
 * 			<td>200</td>
 * 			<td><code>{}</code></td>
 * 			<td><code>{'result': boolean/null}</code></td>
 * 		</tr>
 * 	</tbody>
 * </table>
 * @author Collin
 *
 * @param <T> A game server
 */
public class GameServerCommandHandler<T extends GameServer> extends CommandHandler<T>
{
	public static final String START_COMMAND_NAME = "start";
	public static final String STOP_COMMAND_NAME = "stop";
	public static final String RESTART_COMMAND_NAME = "restart";
	public static final String LOG_COMMAND_NAME = "log";
	public static final String RUNNING_COMMAND_NAME = "running";
	public static final String IP_ADDRESS_COMMAND_NAME = "ipaddress";
	public static final String INPUT_COMMAND_NAME = "input";
	public static final String AUTO_RESTARTS_COMMAND_NAME = "autorestarts";
	
	private final Command startCommand;
	private final Command stopCommand;
	private final Command restartCommand;
	private final Command logCommand;
	private final Command runningCommand;
	private final Command ipAddressCommand;
	private final Command input;
	private final Command autoRestarts;
	
	/**
	 * Constructor. Convenience method for getting an unbound handler. 
	 */
	public GameServerCommandHandler()
	{
		this(null);
	}
	
	/**
	 * Constructor. Binds the server to this handler.
	 * @param server the server to handle commands with
	 */
	public GameServerCommandHandler(T server)
	{
		super(server);
		
		startCommand = new Command(START_COMMAND_NAME)
		{
			@Override
			public HttpStatus apply(JSONObject request, JSONObject response)
			{
				try
				{
					server.startServer();
					return HttpStatus.OK;
				}
				catch(IOException e)
				{
					StartUpApplication.LOGGER.warn(String.format("Unable to start server:\n%s", e.getMessage()));
					return HttpStatus.INTERNAL_SERVER_ERROR;
				}
			}
		};
		
		stopCommand = new Command(STOP_COMMAND_NAME)
		{
			@Override
			public HttpStatus apply(JSONObject request, JSONObject response)
			{
				try
				{
					if(server.stopServer())
					{
						return HttpStatus.OK;
					}
					
					StartUpApplication.LOGGER.warn("Could not stop server successfully");
					return HttpStatus.INTERNAL_SERVER_ERROR;
				} catch (IOException e)
				{
					StartUpApplication.LOGGER.error(String.format("Error stopping server: %s", e.getMessage()));
					return HttpStatus.INTERNAL_SERVER_ERROR;
				}
			}
		};
		
		restartCommand = new Command(RESTART_COMMAND_NAME)
		{
			@Override
			public HttpStatus apply(JSONObject request, JSONObject response)
			{
				try
				{
					server.restartServer();
					return HttpStatus.OK;
				}
				catch(IOException e)
				{
					StartUpApplication.LOGGER.warn(String.format("Unable to restart server:\n%s", e.getMessage()));
					return HttpStatus.INTERNAL_SERVER_ERROR;
				}
			}
		};
		
		logCommand = new Command(LOG_COMMAND_NAME)
		{
			@SuppressWarnings("unchecked")
			@Override
			public HttpStatus apply(JSONObject request, JSONObject response)
			{
				response.put("result", server.getLog());
				return HttpStatus.OK;
			}
		};
		
		runningCommand = new Command(RUNNING_COMMAND_NAME)
		{
			@SuppressWarnings("unchecked")
			@Override
			public HttpStatus apply(JSONObject request, JSONObject response)
			{
				response.put("result", server.isRunning());
				return HttpStatus.OK;
			}
		};
		
		ipAddressCommand = new Command(IP_ADDRESS_COMMAND_NAME)
		{
			@SuppressWarnings("unchecked")
			@Override
			public HttpStatus apply(JSONObject request, JSONObject response)
			{
				response.put("result", server.getPublicIPAddress());
				return HttpStatus.OK;
			}
		};
		
		input = new Command(INPUT_COMMAND_NAME)
		{
			@Override
			public HttpStatus apply(JSONObject request, JSONObject response)
			{
				if(!server.isRunning())
				{
					return HttpStatus.BAD_REQUEST;
				}
				
				try
				{
					if(server.writeToServer((String) request.get("value")))
					{
						return HttpStatus.OK;
					}
					
					return HttpStatus.INTERNAL_SERVER_ERROR;
				} catch (IOException e)
				{
					StartUpApplication.LOGGER.error(String.format("Error writing to server:\n%s", e.getMessage()));
					return HttpStatus.INTERNAL_SERVER_ERROR;
				}
			}
		};
		
		autoRestarts = new Command(AUTO_RESTARTS_COMMAND_NAME)
		{
			@SuppressWarnings("unchecked")
			@Override
			public HttpStatus apply(JSONObject request, JSONObject response)
			{
				try
				{
					response.put("result", server.getGameServerOptions().autoRestarts());
					return HttpStatus.OK;
				} catch (SQLException e)
				{
					StartUpApplication.LOGGER.error(String.format("Unable to check if server auto restarts:\n%s", e.getMessage()));
					return HttpStatus.INTERNAL_SERVER_ERROR;
				}
			}
		};
	}
	
	/**
	 * Gets all the commands of this handler.
	 * @return the list of commands that this handler handles
	 */
	@Override
	public List<Command> getCommands()
	{
		return List.of(
			startCommand,
			stopCommand,
			restartCommand,
			logCommand,
			runningCommand,
			ipAddressCommand,
			input,
			autoRestarts
		);
	}
}