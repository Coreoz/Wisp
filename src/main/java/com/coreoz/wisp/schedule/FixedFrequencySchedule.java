package com.coreoz.wisp.schedule;

import java.time.Duration;

public class FixedFrequencySchedule implements Schedule {

	private final Duration frequency;

	public FixedFrequencySchedule(Duration frequency) {
		this.frequency = frequency;
	}

	@Override
	public long nextExecutionInMillis(long currentTimeInMillis, int executionsCount, Long lastExecutionEndedTimeInMillis) {
		return currentTimeInMillis + frequency.toMillis() - (currentTimeInMillis % frequency.toMillis());
	}

	@Override
	public String toString() {
		return "every " + frequency.toMillis() + "ms";
	}
}
