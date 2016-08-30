package com.coreoz.plume.scheduler;

import java.time.Instant;

import com.coreoz.plume.scheduler.schedule.Schedule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

// TODO remove all lombok annotation to control the public API
@Setter
@Getter
@AllArgsConstructor(staticName = "of")
@Accessors(fluent = true)
public class Job {

	// TODO check if everything really needs to be volatile
	private volatile JobStatus status;
	private volatile Long nextExecutionTimeInMillis;
	private volatile Integer executionsCount;
	private Long lastExecutionTimeInMillis;
	private final String name;
	private final Schedule schedule;
	private final Runnable runnable;

	// package API

	// toString

	@Override
	public String toString() {
		return "Job " + name + " [" + status + "] - " + schedule
				+ " - next execution at " + Instant.ofEpochMilli(nextExecutionTimeInMillis) +"ms";
	}

}
