package com.coreoz.plume.scheduler;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
	private AtomicReference<JobStatus> status;
	private AtomicLong nextExecutionTimeInMillis;
	private AtomicInteger executionsCount;
	private final String name;
	private final Schedule schedule;
	private final Runnable runnable;

	// package API

	// toString

	@Override
	public String toString() {
		return "Job " + name + " [" + status + "] - " + schedule
				+ " - next execution at " + Instant.ofEpochMilli(nextExecutionTimeInMillis.get()) +"ms";
	}

}
