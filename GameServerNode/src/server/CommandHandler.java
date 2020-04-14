package server;

import java.util.List;

import org.json.simple.JSONObject;

import utils.servlet.HttpStatus;

/**
 * Class for handling the input and output of custom
 * commands to and from the generic object. Implementations
 * are responsible for maintaining specific commands
 * and how they interact with the generics that they so choose.
 * 
 * It is up to the user to manage what their own implementations send as a
 * response. It is highly recommended to call the base class's implementation (if one exists)
 * to have a previously defined command take over. Please be aware of the commands
 * that already exist in the class that you are inheriting.
 * @author Collin
 *
 * @param <T> any type
 */
abstract public class CommandHandler <T>
{
	/**
	 * The object to interact with.
	 */
	private T server;
	
	/**
	 * Constructor. Useful when not wanting to
	 * bind to a server right away, or to grab all the commands
	 * without needing a server.
	 */
	public CommandHandler()
	{
	}
	
	/**
	 * Constructor. Takes in an object worth of command handling.
	 * @param server an object that is implementation defined
	 */
	public CommandHandler(T server)
	{
		setServer(server);
	}
	
	/**
	 * Sets the current server to the one given.
	 * @param server a server to bind
	 */
	public void setServer(T server)
	{
		this.server = server;
	}
	
	/**
	 * Gets the currently held object. Since this class
	 * is mainly for game servers, the name of this method
	 * is for understandability.
	 * @return the currently held game server (or whatever generic you have), or null if one doesn't exist
	 */
	public T getServer()
	{
		return server;
	}
	
	/**
	 * Responsible for parsing the given command and request and filling
	 * the given JSON object with values that make sense for the application.
	 * Implementations that defined this class will inherently have their
	 * implementations called when their associating servers are being pinged
	 * for a command. 
	 * @param command the command keyword of meaning
	 * @param request a request object containing possible user parameters
	 * @param response add information to the response through a JSON object format
	 * to be used by the receiver.
	 * @return an http status code that signals to the client about the status of the request and response
	 */
	public HttpStatus command(JSONObject request, JSONObject response)
	{
		for(var commandImpl : getCommands())
		{
			if(commandImpl.getCommandName().equals(request.get("command")))
			{
				return commandImpl.apply(request, response);
			}
		}
		
		return HttpStatus.NOT_IMPLEMENTED;
	}
	
	/**
	 * Gets all the commands of this command handler.
	 * It is recommended to call the base class (if one exists)
	 * to get a full list of commands that are supported by this handler.
	 * @return a list of supported commands
	 */
	abstract public List<Command> getCommands();
}
