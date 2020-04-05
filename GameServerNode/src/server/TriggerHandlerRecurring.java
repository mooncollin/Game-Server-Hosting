package server;

import java.time.Duration;
import java.util.TimerTask;
import java.util.regex.Pattern;

import utils.TimerTaskID;
import utils.Utils;

public class TriggerHandlerRecurring extends TriggerHandler
{
	public static final Pattern RECURRING_PATTERN = Pattern.compile("(?<hour>[01]?[0-9]|2[0-3]):(?<minute>[0-5][0-9]):(?<second>[0-5][0-9])");
	
	public static String convertSecondsToFormat(long seconds)
	{
		return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
	}
	
	public static long convertFormatToSeconds(String str)
	{
		var matcher = RECURRING_PATTERN.matcher(str);
		if(!matcher.matches())
		{
			return -1;
		}
		
		var hour = Utils.fromString(Integer.class, matcher.group("hour")).orElseGet(() -> 0);
		var minute = Utils.fromString(Integer.class, matcher.group("minute")).orElseGet(() -> 0);
		var second = Utils.fromString(Integer.class, matcher.group("second")).orElseGet(() -> 0);
		
		return Duration.ZERO.plusHours(hour).plusMinutes(minute).plusSeconds(second).getSeconds();
	}
	
	private int seconds;
	
	public TriggerHandlerRecurring(GameServer server, String command, String action, long id, int seconds)
	{
		super(server, command, action, id);
		setRecurringPeriod(seconds);
	}
	
	public void setRecurringPeriod(int seconds)
	{
		this.seconds = seconds;
	}
	
	public int getRecurringPeriod()
	{
		return seconds;
	}
	
	public TimerTask generateTimerTask()
	{
		return new TimerTaskID(getID()) {
			public void run() {
				trigger();
			}
		};
	}
}
