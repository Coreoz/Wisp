package com.coreoz.wisp.schedule;

import java.time.Duration;

public class AfterInitialDelaySchedule implements Schedule {

	private final Schedule baseSchedule;
	private final Duration initialDelay;
	private boolean hasNotBeenExecuted;

	public AfterInitialDelaySchedule(Schedule baseSchedule, Duration initialDelay) {
		this.baseSchedule = baseSchedule;
		this.initialDelay = initialDelay;
		this.hasNotBeenExecuted = true;
	}

	@Override
	public long nextExecutionInMillis(long currentTimeInMillis, int executionsCount, Long lastExecutionTimeInMillis) {
		if(hasNotBeenExecuted) {
			hasNotBeenExecuted = false;
			return initialDelay.toMillis() + currentTimeInMillis;
		}
		return baseSchedule.nextExecutionInMillis(currentTimeInMillis, executionsCount, lastExecutionTimeInMillis);
	}

	@Override
	public String toString() {
		if(hasNotBeenExecuted) {
			return "first after " + initialDelay + ", then " + baseSchedule;
		}
		return baseSchedule.toString();
	}

}
