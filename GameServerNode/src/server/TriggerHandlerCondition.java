package server;

import java.util.Objects;
import java.util.function.Predicate;

public class TriggerHandlerCondition<T> extends TriggerHandler
{
	private Predicate<T> condition;
	private TriggerHandlerConditionType type;
	
	public TriggerHandlerCondition(GameServer server, String command, String action, long id, Predicate<T> condition, TriggerHandlerConditionType type)
	{
		super(server, command, action, id);
		setCondition(condition);
		setType(type);
	}
	
	public void setCondition(Predicate<T> condition)
	{	
		this.condition = Objects.requireNonNull(condition);
	}
	
	public void setType(TriggerHandlerConditionType type)
	{
		this.type = Objects.requireNonNull(type);
	}
	
	public Predicate<T> getCondition()
	{
		return condition;
	}
	
	public TriggerHandlerConditionType getType()
	{
		return type;
	}
	
	public void trigger(T input)
	{
		if(condition.test(input))
		{
			trigger();
		}
	}
	
	public enum TriggerHandlerConditionType
	{
		OUTPUT
	}
}
