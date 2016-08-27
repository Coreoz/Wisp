package com.coreoz.plume.scheduler.schedule;

import com.coreoz.plume.scheduler.time.TimeProvider;

public class OnceSchedule implements Schedule {

	private static final long WILL_NOT_BE_EXECUTED_AGAIN = -1L;

	private final Schedule baseSchedule;
	private boolean executed;

	public OnceSchedule(Schedule baseSchedule) {
		this.baseSchedule = baseSchedule;
		this.executed = false;
	}

	@Override
	public long nextExecutionInMillis(TimeProvider timeProvider) {
		if(executed) {
			return WILL_NOT_BE_EXECUTED_AGAIN;
		}
		executed = true;
		return baseSchedule.nextExecutionInMillis(timeProvider);
	}

	@Override
	public String toString() {
		return "once " + baseSchedule;
	}

}
