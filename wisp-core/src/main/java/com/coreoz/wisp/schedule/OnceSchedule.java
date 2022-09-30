package com.coreoz.wisp.schedule;

public class OnceSchedule implements Schedule {

	private final Schedule baseSchedule;
	private Integer initialExecutionsCount;

	public OnceSchedule(Schedule baseSchedule) {
		this.baseSchedule = baseSchedule;
		this.initialExecutionsCount = null;
	}

	@Override
	public long nextExecutionInMillis(long currentTimeInMillis, int executionsCount, Long lastExecutionTimeInMillis) {
		if(initialExecutionsCount == null) {
			initialExecutionsCount = executionsCount;
		}
		if(initialExecutionsCount < executionsCount) {
			return WILL_NOT_BE_EXECUTED_AGAIN;
		}
		return baseSchedule.nextExecutionInMillis(currentTimeInMillis, executionsCount, lastExecutionTimeInMillis);
	}

	public Schedule baseSchedule() {
		return baseSchedule;
	}

	@Override
	public String toString() {
		return "once, " + baseSchedule;
	}

}
