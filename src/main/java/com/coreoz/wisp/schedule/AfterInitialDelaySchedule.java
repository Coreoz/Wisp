package com.coreoz.wisp.schedule;

import java.time.Duration;

public class AfterInitialDelaySchedule implements Schedule {

	private final Schedule baseSchedule;
	private final Duration initialDelay;
	private Integer initialExecutionsCount;
	private boolean hasNotBeenExecuted;

	public AfterInitialDelaySchedule(Schedule baseSchedule, Duration initialDelay) {
		this.baseSchedule = baseSchedule;
		this.initialDelay = initialDelay;
		this.initialExecutionsCount = null;
		this.hasNotBeenExecuted = true;
	}

	@Override
	public long nextExecutionInMillis(long currentTimeInMillis, int executionsCount, Long lastExecutionTimeInMillis) {
		if(initialExecutionsCount == null) {
			initialExecutionsCount = executionsCount;
		}
		if(initialExecutionsCount >= executionsCount) {
			return initialDelay.toMillis() + currentTimeInMillis;
		}
		hasNotBeenExecuted = false;
		return baseSchedule.nextExecutionInMillis(currentTimeInMillis, executionsCount, lastExecutionTimeInMillis);
	}

	public Schedule baseSchedule() {
		return baseSchedule;
	}

	@Override
	public String toString() {
		if(hasNotBeenExecuted) {
			return "first after " + initialDelay + ", then " + baseSchedule;
		}
		return baseSchedule.toString();
	}

}
