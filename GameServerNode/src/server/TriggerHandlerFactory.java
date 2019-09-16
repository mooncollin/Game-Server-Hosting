package server;

import java.time.LocalTime;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import server.TriggerHandlerCondition.TriggerHandlerConditionType;

public class TriggerHandlerFactory
{	
	public static TriggerHandler getSpecificTriggerHandler(models.Triggers trigger, GameServer server)
	{
		Objects.requireNonNull(trigger);
		TriggerHandler specificTrigger = null;

		try
		{
			if(trigger.getType().equals("time"))
			{
				specificTrigger = new TriggerHandlerTime(server, trigger.getCommand(), trigger.getExtra(), trigger.getID(), LocalTime.MIDNIGHT.plusSeconds(Integer.valueOf(trigger.getValue())));
			}
			else if(trigger.getType().equals("output"))
			{
				if(trigger.getValue().startsWith("r/") && trigger.getValue().endsWith("/"))
				{
					String regexValue = trigger.getValue().replaceFirst("r/", "");
					regexValue = regexValue.substring(0, regexValue.lastIndexOf("/"));
					Pattern p = Pattern.compile(".*?" + regexValue + ".*");
					specificTrigger = new TriggerHandlerCondition<String>(server, trigger.getCommand(), trigger.getExtra(), trigger.getID(), p.asPredicate(), TriggerHandlerConditionType.OUTPUT);
				}
				else
				{
					specificTrigger = new TriggerHandlerCondition<String>(server, trigger.getCommand(), trigger.getExtra(), trigger.getID(), new Predicate<String>() {
						public boolean test(String value)
						{
							return value != null && value.contains(trigger.getValue());
						}
					}, TriggerHandlerConditionType.OUTPUT);
				}
			}
			else if(trigger.getType().equals("recurring"))
			{
				specificTrigger = new TriggerHandlerRecurring(server, trigger.getCommand(), trigger.getExtra(), trigger.getID(), Integer.parseInt(trigger.getValue()));
			}
		}
		catch(Exception e)
		{
			specificTrigger = null;
		}
		
		return specificTrigger;
	}
}
