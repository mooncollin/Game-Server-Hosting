package utils;

import java.util.TimerTask;

abstract public class TimerTaskID extends TimerTask
{
	private long id;
	
	public TimerTaskID(long id)
	{
		this.id = id;
	}
	
	public long getID()
	{
		return id;
	}
}
