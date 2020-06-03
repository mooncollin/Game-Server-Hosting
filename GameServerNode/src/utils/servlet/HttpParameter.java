package utils.servlet;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletRequest;

import utils.Utils;
import utils.functional.True;

/**
 * Class for creating rules for how an http parameter is parsed.
 * @author Collin
 *
 * @param <T>
 */
public class HttpParameter<T>
{
	/**
	 * Name of the parameter.
	 */
	private final String name;
	
	/**
	 * Whether this parameter is nullable.
	 */
	private final boolean nullable;
	
	/**
	 * A function to validate a parsed parameter.
	 */
	private final Predicate<T> checker;
	
	/**
	 * A function to parse a string into a given
	 * parameter.
	 */
	private final Function<String, Optional<T>> parser;
	
	/**
	 * Constructor. Sets up variables.
	 * @param clazz the class type of the generic argument
	 * @param name the name of the parameter
	 * @param nullable whether the parameter is nullable
	 */
	public HttpParameter(Class<T> clazz, String name, boolean nullable)
	{
		this(clazz, name, nullable, null, null);
	}
	
	/**
	 * Constructor. Sets up variables.
	 * @param clazz the class type of the generic argument
	 * @param name the name of the parameter
	 * @param nullable whether the parameter is nullable
	 * @param checker a validation function
	 * Defaults the parser to attempt to convert the generic type into a string.
	 */
	public HttpParameter(Class<T> clazz, String name, boolean nullable, Predicate<T> checker)
	{
		this(clazz, name, nullable, checker, null);
	}
	
	/**
	 * Constructor. Sets up variables.
	 * @param clazz the class type of the generic argument
	 * @param name the name of the parameter
	 * @param nullable whether the parameter is nullable
	 * @param parser a parsing function
	 * Defaults the checker to return true always.
	 */
	public HttpParameter(Class<T> clazz, String name, boolean nullable, Function<String, Optional<T>> parser)
	{
		this(clazz, name, nullable, null, parser);
	}
	
	/**
	 * Constructor. Sets up variables.
	 * @param clazz the class type of the generic argument
	 * @param name the name of the parameter
	 * @param nullable whether the parameter is nullable
	 * @param checker a validation function
	 * @param parser a parsing function
	 */
	public HttpParameter(Class<T> clazz, String name, boolean nullable, Predicate<T> checker, Function<String, Optional<T>> parser)
	{
		this.name = Objects.requireNonNull(name);
		this.nullable = nullable;
		this.checker = Objects.requireNonNullElse(checker, new True<T>());
		this.parser = Objects.requireNonNullElse(parser, str -> Utils.fromString(clazz, str));
	}
	
	/**
	 * Checks if the parameter is nullable.
	 * @return true if nullable, false otherwise
	 */
	public boolean isNullable()
	{
		return nullable;
	}
	
	/**
	 * Gets the name of the parameter.
	 * @return parameter name
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Gets the validation function.
	 * @return validation function
	 */
	public Predicate<T> getChecker()
	{
		return checker;
	}
	
	/**
	 * Checks if the given value is valid by checking against
	 * its nullable rule and its validation function.
	 * @param value the value to check
	 * @return true if the given value is valid, false otherwise
	 */
	public boolean valid(final T value)
	{
		if(!isNullable() && value == null)
		{
			return false;
		}

		return checker.test(value);
	}
	
	/**
	 * Parses a string into the value of the generic type by
	 * using its parsing function. If the parsing function fails
	 * or the validation fails, it returns an empty optional.
	 * @param str the string to parse
	 * @return a filled optional if parsing and validation passes,
	 * an empty optional otherwise
	 */
	public Optional<T> parse(String str)
	{
		var<T> optionValue = parser.apply(str);
		if(optionValue.isEmpty() || !valid(optionValue.get()))
		{
			return Optional.empty();
		}
		
		return optionValue;
	}
	
	/**
	 * Parses the parameter from the given request that corresponds
	 * with the name of this parameter.
	 * @param request an http request
	 * @return a filled optional if parsing and validation passes,
	 * an empty optional otherwise
	 */
	public Optional<T> parse(HttpServletRequest request)
	{
		return parse(request.getParameter(getName()));
	}
}
