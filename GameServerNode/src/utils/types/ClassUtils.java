package utils.types;

import java.util.Collections;
import java.util.List;

public class ClassUtils
{
	private ClassUtils() {}
	
	@SuppressWarnings("unchecked")
	public static <T> Class<List<T>> listClass()
	{
		return (Class<List<T>>) Collections.<T>emptyList().getClass();
	}
}
