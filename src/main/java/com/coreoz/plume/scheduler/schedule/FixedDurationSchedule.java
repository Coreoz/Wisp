package com.coreoz.plume.scheduler.schedule;

public class FixedDurationSchedule implements Schedule {

	private final long intervalInMillis;

	public FixedDurationSchedule(long intervalInMillis) {
		this.intervalInMillis = intervalInMillis;
	}

	@Override
	public long nextExecutionInMillis(int executionsCount, long currentTimeInMillis) {
		return currentTimeInMillis + intervalInMillis;
	}

	@Override
	public String toString() {
		return "every " + intervalInMillis + "ms";
	}

}
