package utils.functional;

import java.util.function.Predicate;

public class True <T> implements Predicate<T>
{
	public boolean test(T value)
	{
		return true;
	}
}
