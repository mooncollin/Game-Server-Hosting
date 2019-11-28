package frontend.javascript;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class JavascriptMap <K, V> extends JavascriptVariable<Map<K, V>>
{	
	public JavascriptMap(String name)
	{
		super(name, new HashMap<K, V>());
	}
	
	public void clear()
	{
		getValue().clear();
	}
	
	public boolean delete(K key)
	{
		return getValue().remove(key) != null;
	}
	
	public Set<Map.Entry<K, V>> entries()
	{
		return getValue().entrySet();
	}
	
	public void forEach(BiConsumer<K, V> callback)
	{
		getValue().forEach(callback);
	}
	
	public V get(K key)
	{
		return getValue().get(key);
	}
	
	public boolean has(K key)
	{
		return getValue().containsKey(key);
	}
	
	public Set<K> keys()
	{
		return getValue().keySet();
	}
	
	public Map<K, V> set(K key, V value)
	{
		getValue().put(key, value);
		return getValue();
	}
	
	public Collection<V> values()
	{
		return getValue().values();
	}
	
	@Override
	public String toString()
	{
		return String.format("var %s = {%s};", getName(),
			String.join(", ", getValue().entrySet().stream().map(entry -> {
				var keyString = String.valueOf(entry.getKey());
				var valueString = String.valueOf(entry.getValue());
				
				if(entry.getKey() instanceof String)
				{
					keyString = '"' + keyString.replace("\"", "\\\"") + '"';
				}
				if(entry.getValue() instanceof String)
				{
					valueString = '"' + valueString.replace("\"", "\\\"") + '"';
				}
				
				return String.format("%s : %s", keyString, valueString);
			}).toArray(String[]::new)));
	}
}
