package server;

import java.util.TimerTask;

import utils.TimerTaskID;

public class TriggerHandlerRecurring extends TriggerHandler
{
	private int seconds;
	
	public TriggerHandlerRecurring(GameServer server, String command, String action, long id, int minutes)
	{
		super(server, command, action, id);
		setRecurringPeriod(minutes);
	}
	
	public void setRecurringPeriod(int minutes)
	{
		this.seconds = minutes;
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
