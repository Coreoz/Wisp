package com.coreoz.wisp.schedule;

import java.time.Duration;

public class FixedDelaySchedule implements Schedule {

	private final Duration frequency;

	public FixedDelaySchedule(Duration frequency) {
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
