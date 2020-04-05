package utils;

import java.util.Set;
import java.util.function.Predicate;

public class Options <T extends Comparable<T>> implements Predicate<T>
{
	private Set<T> options;
	
	@SafeVarargs
	public Options(T... options)
	{
		this.options = Set.of(options);
	}
	
	public Set<T> getOptions()
	{
		return options;
	}
	
	public boolean test(T value)
	{
		return options.contains(value);
	}
}
