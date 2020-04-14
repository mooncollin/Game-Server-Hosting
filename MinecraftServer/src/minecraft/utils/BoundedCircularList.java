package minecraft.utils;

import java.lang.reflect.Array;
import java.util.function.IntFunction;

public class BoundedCircularList<T>
{
	private T[] data;
	private int start;
	private int size;
	private int end;
	
	@SuppressWarnings("unchecked")
	public BoundedCircularList(int capacity)
	{
		if(capacity <= 0)
		{
			throw new IllegalArgumentException();
		}
		
		data = (T[]) new Object[capacity];
		clear();
	}
	
	public void add(T item)
	{
		data[end % getCapacity()] = item;
		end = (end + 1) % getCapacity();
		if(end == start)
		{
			start = (start + 1) % getCapacity();
		}
		if(size < getCapacity())
		{
			size++;
		}
	}
	
	public Object[] toArray()
	{
		Object[] copiedData = new Object[getSize()];
		int copyStart = 0;
		for(int i = start; i < size; i++, copyStart++)
		{
			copiedData[copyStart] = data[i];
		}
		if(end < start)
		{
			for(int i = 0; i < end; i++, copyStart++)
			{
				copiedData[copyStart] = data[i];
			}
		}
		
		return copiedData;
	}
	
	public T[] toArray(IntFunction<T[]> sup)
	{
		return toArray(sup.apply(0));
	}
	
	@SuppressWarnings("unchecked")
	public T[] toArray(T[] a)
	{
		T[] copiedData = (T[]) Array.newInstance(a.getClass().getComponentType(), getSize());
		int copyStart = 0;
		for(int i = start; i < size; i++, copyStart++)
		{
			copiedData[copyStart] = data[i];
		}
		if(end < start)
		{
			for(int i = 0; i < end; i++, copyStart++)
			{
				copiedData[copyStart] = data[i];
			}
		}
		
		return copiedData;
	}
	
	public T get(int index)
	{
		if(index < 0 || index >= size)
		{
			throw new IndexOutOfBoundsException(String.valueOf(index));
		}
		
		return data[index];
	}
	
	public int getCapacity()
	{
		return data.length;
	}
	
	public int getSize()
	{
		return size;
	}
	
	public void clear()
	{
		start = end = size = 0;
	}
}
