package server;

import java.util.TimerTask;

import utils.TimerTaskID;

public class TriggerHandlerRecurring extends TriggerHandler
{
	public static String convertSecondsToFormat(long seconds)
	{
		return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
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
