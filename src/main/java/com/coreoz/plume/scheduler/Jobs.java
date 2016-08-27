package com.coreoz.plume.scheduler;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
class Jobs {

	private final Map<String, Job> indexedByName;
	private final TreeSet<Job> nextExecutionsOrder;

	private RunningJob nextRunningJob;

	Jobs() {
		nextRunningJob = null;
		indexedByName = new ConcurrentHashMap<>();
		nextExecutionsOrder = new TreeSet<>(Comparator.comparing(Job::nextExecutionTimeInMillis));
	}

}
