package com.coreoz.wisp.schedule;

public class OnceSchedule implements Schedule {

	private final Schedule baseSchedule;

	public OnceSchedule(Schedule baseSchedule) {
		this.baseSchedule = baseSchedule;
	}

	@Override
	public long nextExecutionInMillis(long currentTimeInMillis, int executionsCount, Long lastExecutionTimeInMillis) {
		if(executionsCount > 0) {
			return WILL_NOT_BE_EXECUTED_AGAIN;
		}
		return baseSchedule.nextExecutionInMillis(currentTimeInMillis, executionsCount, lastExecutionTimeInMillis);
	}

	@Override
	public String toString() {
		return "once, " + baseSchedule;
	}

}
