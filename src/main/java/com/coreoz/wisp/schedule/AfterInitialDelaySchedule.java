package com.coreoz.wisp.schedule;

import java.time.Duration;

public class AfterInitialDelaySchedule implements Schedule {

	private final Schedule baseSchedule;
	private final Duration initialDelay;

	public AfterInitialDelaySchedule(Schedule baseSchedule, Duration initialDelay) {
		this.baseSchedule = baseSchedule;
		this.initialDelay = initialDelay;
	}

	@Override
	public long nextExecutionInMillis(int executionsCount, long currentTimeInMillis) {
		if(executionsCount == 0) {
			return initialDelay.toMillis() + currentTimeInMillis;
		}
		return baseSchedule.nextExecutionInMillis(executionsCount, currentTimeInMillis);
	}

	@Override
	public String toString() {
		return "first after " + initialDelay + ", then " + baseSchedule;
	}

}
