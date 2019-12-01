package server;

import java.time.LocalTime;
import java.util.Objects;
import java.util.TimerTask;

import utils.TimerTaskID;

public class TriggerHandlerTime extends TriggerHandler
{
	public static String convertSecondsToFormat(long seconds)
	{
		return LocalTime.MIDNIGHT.plusSeconds(seconds).toString();
	}
	
	private LocalTime timeExecuted;
	
	public TriggerHandlerTime(GameServer server, String command, String action, long id, LocalTime timeExecuted)
	{
		super(server, command, action, id);
		setTimeExecuted(timeExecuted);
	}
	
	public void setTimeExecuted(LocalTime timeExecuted)
	{
		this.timeExecuted = Objects.requireNonNull(timeExecuted);
	}
	
	public LocalTime getTimeExecuted()
	{
		return timeExecuted;
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
