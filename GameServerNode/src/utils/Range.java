package utils;

import java.util.Objects;
import java.util.function.Predicate;

public class Range <T extends Comparable<T>> implements Predicate<T>
{
	private T min;
	private T max;
	
	public Range(T min, T max)
	{
		min = Objects.requireNonNull(min);
		max = Objects.requireNonNull(max);
	}
	public T getMin()
	{
		return min;
	}
	
	public T getMax()
	{
		return max;
	}
	
	public boolean test(T value)
	{
		return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
	}
}
