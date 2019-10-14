package server;

import java.time.LocalTime;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import model.Table;
import models.TriggersTable;
import server.TriggerHandlerCondition.TriggerHandlerConditionType;

public class TriggerHandlerFactory
{	
	public static TriggerHandler getSpecificTriggerHandler(Table trigger, GameServer server)
	{
		Objects.requireNonNull(trigger);
		Objects.requireNonNull(server);
		TriggerHandler specificTrigger = null;
		
		var triggerType		= trigger.getColumnValue(TriggersTable.TYPE);
		var triggerCommand	= trigger.getColumnValue(TriggersTable.COMMAND);
		var triggerValue	= trigger.getColumnValue(TriggersTable.VALUE);
		var triggerID		= trigger.getColumnValue(TriggersTable.ID);
		var triggerExtra	= trigger.getColumnValue(TriggersTable.EXTRA);

		try
		{
			if(triggerType.equals("time"))
			{
				specificTrigger = new TriggerHandlerTime(server, triggerCommand, triggerExtra, triggerID, LocalTime.MIDNIGHT.plusSeconds(Integer.valueOf(triggerValue)));
			}
			else if(triggerType.equals("output"))
			{
				if(triggerValue.startsWith("r/") && triggerValue.endsWith("/"))
				{
					String regexValue = triggerValue.replaceFirst("r/", "");
					regexValue = regexValue.substring(0, regexValue.lastIndexOf("/"));
					Pattern p = Pattern.compile(".*?" + regexValue + ".*");
					specificTrigger = new TriggerHandlerCondition<String>(server, triggerCommand, triggerExtra, triggerID, p.asPredicate(), TriggerHandlerConditionType.OUTPUT);
				}
				else
				{
					specificTrigger = new TriggerHandlerCondition<String>(server, triggerCommand, triggerExtra, triggerID, new Predicate<String>() {
						public boolean test(String value)
						{
							return value != null && value.contains(triggerValue);
						}
					}, TriggerHandlerConditionType.OUTPUT);
				}
			}
			else if(triggerType.equals("recurring"))
			{
				specificTrigger = new TriggerHandlerRecurring(server, triggerCommand, triggerExtra, triggerID, Integer.parseInt(triggerValue));
			}
		}
		catch(Exception e)
		{
			specificTrigger = null;
		}
		
		return specificTrigger;
	}
}
