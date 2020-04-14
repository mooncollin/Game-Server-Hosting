package server;

import java.util.function.BiFunction;

import org.json.simple.JSONObject;

import utils.servlet.HttpStatus;

/**
 * Helpful class that can be used to easily do command actions.
 * @author Collin
 *
 */
abstract public class Command implements BiFunction<JSONObject, JSONObject, HttpStatus>
{
	/**
	 * The command name of this command.
	 */
	private final String commandName;
	
	/**
	 * Constructor. Sets the command name.
	 * @param commandName the command name to associate this command with
	 */
	public Command(String commandName)
	{
		this.commandName = commandName;
	}
	
	/**
	 * Gets the current command name.
	 * @return the command name
	 */
	public String getCommandName()
	{
		return commandName;
	}
}