package utils;

import java.util.Arrays;
import java.util.Objects;

public class Tuple implements Cloneable
{
	private final Object[] objs;
	
	public Tuple(Object... objs)
	{
		Objects.requireNonNull(objs);
		this.objs = new Object[objs.length];
		Arrays.parallelSetAll(this.objs, (int i) -> objs[i]);
	}
	
	public Object[] getTuple()
	{
		return objs;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getValue(int index)
	{
		return (T) objs[index];
	}
	
	public Class<?> getType(int index)
	{
		return objs[index].getClass();
	}
	
	public int length()
	{
		return objs.length;
	}
	
	@Override
	public int hashCode()
	{
		return objs.hashCode();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if(!other.getClass().isArray())
		{
			return false;
		}
		
		return Arrays.equals(objs, (Object[]) other);
	}
	
	@Override
	public Object clone()
	{
		return (Object) new Tuple(Arrays.copyOf(objs, objs.length));
	}
}
