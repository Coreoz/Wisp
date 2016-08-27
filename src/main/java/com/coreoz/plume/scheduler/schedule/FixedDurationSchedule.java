package com.coreoz.plume.scheduler.schedule;

import com.coreoz.plume.scheduler.time.TimeProvider;

public class FixedDurationSchedule implements Schedule {

	private final long intervalInMillis;

	public FixedDurationSchedule(long intervalInMillis) {
		this.intervalInMillis = intervalInMillis;
	}

	@Override
	public long nextExecutionInMillis(TimeProvider timeProvider) {
		return timeProvider.currentTime() + intervalInMillis;
	}

	@Override
	public String toString() {
		return "every " + intervalInMillis + "ms";
	}

}
