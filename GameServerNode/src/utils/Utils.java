package utils;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.BiFunction;
import java.lang.reflect.InvocationTargetException;

public class Utils
{
	public static int map(int in, int in_min, int in_max, int out_min, int out_max)
	{
		var denom = in_max - in_min;
		if(denom == 0)
		{
			return 0;
		}
		
		return (in - in_min) * (out_max - out_min) / denom + out_min;
	}
	
	public static <T extends Comparable<T>> T clamp(T value, T min, T max)
	{
		return value.compareTo(min) < 0 ? min : value.compareTo(max) > 0 ? max : value;
	}
	
	public static <T, U> U optionalEvaluateOr(Optional<T> option, Function<? super T, U> action, Supplier<U> emptyAction)
	{
		if(option.isPresent())
		{
			return action.apply(option.get());
		}
		
		return emptyAction.get();
	}
	
	public static boolean optionalsPresent(Optional<?>... optionals)
	{
		return Arrays.stream(optionals).allMatch(Optional::isPresent);
	}
	
	public static <T, R> Supplier<R> bind(Function<T, R> func, T value)
	{
		return new Supplier<R>() {
			public R get()
			{
				return func.apply(value);
			}
		};
	}
	
	public static <T, U, R> Supplier<R> bind(BiFunction<T, U, R> func, T firstValue, U secondValue)
	{
		return new Supplier<R>() {
			public R get()
			{
				return func.apply(firstValue, secondValue);
			}
		};
	}
	
	public static <T> Optional<T> fromString(Class<T> clazz, String str)
	{
		if(str == null)
		{
			return Optional.empty();
		}
		
		try
		{
			return Optional.of(clazz.getConstructor(String.class).newInstance(str));
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e)
		{
			return Optional.empty();
		}
	}
	
	@SuppressWarnings("unchecked")
	@SafeVarargs
	public static <T> T[] concatenate(T[] arr, T... objs)
	{
		return concatenate(arr, (T[][]) new Object[][] {objs});
	}
	
	@SafeVarargs
	public static <T> T[] concatenate(T[] arr, T[]... arrs)
	{
		var newLength = arr.length + Arrays.stream(arrs).mapToInt(a -> a.length).sum();
		var newArr = Arrays.copyOf(arr, newLength);
		int i = arr.length;
		
		for(int j = 0; j < arrs.length; j++)
		{
			for(int k = 0; k < arrs[j].length; k++, i++)
			{
				newArr[i] = arrs[j][k];
			}
		}
		
		return newArr;
	}
	
	public static <T> T lastOf(T[] arr, int n)
	{
		return arr[arr.length - n];
	}
}
