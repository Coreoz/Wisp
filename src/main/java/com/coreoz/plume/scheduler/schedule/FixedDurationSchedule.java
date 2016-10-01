package com.coreoz.plume.scheduler.schedule;

import java.time.Duration;

public class FixedDurationSchedule implements Schedule {

	private final Duration frequency;

	public FixedDurationSchedule(Duration frequency) {
		this.frequency = frequency;
	}

	@Override
	public long nextExecutionInMillis(int executionsCount, long currentTimeInMillis) {
		return currentTimeInMillis + frequency.toMillis();
	}

	@Override
	public String toString() {
		return "every " + frequency;
	}

}
