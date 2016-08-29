package com.coreoz.plume.scheduler;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
class Jobs {

	private final Map<String, Job> indexedByName;
	private final ArrayList<Job> nextExecutionsOrder;

	private volatile RunningJob nextRunningJob;

	Jobs() {
		nextRunningJob = null;
		indexedByName = new ConcurrentHashMap<>();
		nextExecutionsOrder = new ArrayList<>();
	}

}
