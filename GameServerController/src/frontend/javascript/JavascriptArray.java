package frontend.javascript;

import java.util.ArrayList;

public class JavascriptArray <T> extends JavascriptVariable<ArrayList<T>>
{	
	@SafeVarargs
	public JavascriptArray(String variableName, T... startingElements)
	{
		super(variableName, new ArrayList<T>());
		addElements(startingElements);
	}
	
	@SuppressWarnings("unchecked")
	public void addElements(T... elements)
	{
		for(var element : elements)
		{
			add(element);
		}
	}
	
	public void add(T element)
	{
		getValue().add(element);
	}
	
	public void removeElement(T element)
	{
		getValue().remove(element);
	}
	
	public T removeElement(int index)
	{
		return getValue().remove(index);
	}
	
	@SuppressWarnings("unchecked")
	public T[] getElements()
	{
		return (T[]) getValue().toArray();
	}
	
	@Override
	public String valueString()
	{
		return String.format("[%s]", String.join(", ", getValue().stream().map(element -> {
			if(element instanceof String)
			{
				return escapeString((String) element);
			}
			else if(element instanceof JavascriptVariable)
			{
				return ((JavascriptVariable<?>) element).valueString();
			}
			
			return String.valueOf(element);
		}).toArray(String[]::new)));
	}
}
