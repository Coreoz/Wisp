package com.coreoz.wisp;

import java.time.Instant;

import com.coreoz.wisp.schedule.Schedule;

/**
 * A {@code Job} is the association of a {@link Runnable} process
 * and its running {@link Schedule}.<br/>
 * <br/>
 * A {@code Job} also contains information about its status and its running
 * statistics.
 */
public class Job {

	private JobStatus status;
	private volatile long nextExecutionTimeInMillis;
	private volatile int executionsCount;
	private Long lastExecutionTimeInMillis;
	private final String name;
	private Schedule schedule;
	private final Runnable runnable;

	// public API

	public JobStatus status() {
		return status;
	}

	public long nextExecutionTimeInMillis() {
		return nextExecutionTimeInMillis;
	}

	public int executionsCount() {
		return executionsCount;
	}

	public Long lastExecutionTimeInMillis() {
		return lastExecutionTimeInMillis;
	}

	public String name() {
		return name;
	}

	public Schedule schedule() {
		return schedule;
	}

	public Runnable runnable() {
		return runnable;
	}

	// package API

	Job(JobStatus status, long nextExecutionTimeInMillis, int executionsCount,
			Long lastExecutionTimeInMillis, String name, Schedule schedule, Runnable runnable) {
		this.status = status;
		this.nextExecutionTimeInMillis = nextExecutionTimeInMillis;
		this.executionsCount = executionsCount;
		this.lastExecutionTimeInMillis = lastExecutionTimeInMillis;
		this.name = name;
		this.schedule = schedule;
		this.runnable = runnable;
	}

	Job status(JobStatus status) {
		this.status = status;
		return this;
	}

	Job nextExecutionTimeInMillis(long nextExecutionTimeInMillis) {
		this.nextExecutionTimeInMillis = nextExecutionTimeInMillis;
		return this;
	}

	Job executionsCount(int executionsCount) {
		this.executionsCount = executionsCount;
		return this;
	}

	Job lastExecutionTimeInMillis(Long lastExecutionTimeInMillis) {
		this.lastExecutionTimeInMillis = lastExecutionTimeInMillis;
		return this;
	}

	void schedule(Schedule schedule) {
		this.schedule = schedule;
	}

	// toString

	@Override
	public String toString() {
		return "Job " + name + " [" + status + "] - will run " + schedule
				+ " - next execution at " + Instant.ofEpochMilli(nextExecutionTimeInMillis);
	}

}
