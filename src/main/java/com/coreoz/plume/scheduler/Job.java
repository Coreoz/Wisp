package com.coreoz.plume.scheduler;

import java.time.Instant;

import com.coreoz.plume.scheduler.schedule.Schedule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@AllArgsConstructor(staticName = "of")
@Accessors(fluent = true)
public class Job {

	private JobStatus status;
	private long nextExecutionTimeInMillis;
	private final String name;
	private final Schedule schedule;
	private final Runnable runnable;

	// package API

	// TODO ajouter en package API l'accès en écriture à status et nextExecutionTime



	// toString

	@Override
	public String toString() {
		return "Job " + name + " [" + status + "] - " + schedule
				+ " - next execution at " + Instant.ofEpochMilli(nextExecutionTimeInMillis) +"ms";
	}

}
