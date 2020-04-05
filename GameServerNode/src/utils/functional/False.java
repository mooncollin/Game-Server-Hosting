package utils.functional;

import java.util.function.Predicate;

public class False <T> implements Predicate<T>
{
	public boolean test(T value)
	{
		return false;
	}
}
