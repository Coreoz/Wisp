package com.coreoz.plume.scheduler.schedule;

import com.coreoz.plume.scheduler.time.TimeProvider;

public class OnceSchedule implements Schedule {

	private final Schedule baseSchedule;

	public OnceSchedule(Schedule baseSchedule) {
		this.baseSchedule = baseSchedule;
	}

	@Override
	public long nextExecutionInMillis(int executionsCount, TimeProvider timeProvider) {
		if(executionsCount > 0) {
			return WILL_NOT_BE_EXECUTED_AGAIN;
		}
		return baseSchedule.nextExecutionInMillis(executionsCount, timeProvider);
	}

	@Override
	public String toString() {
		return "once " + baseSchedule;
	}

}
