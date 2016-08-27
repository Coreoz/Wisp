package com.coreoz.plume.scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * No synchronization is done here because it is handled in the scheduler.
 */
class JobThreadPool {

	private final int maxSize;
	private final List<JobThread> available;
	private final Set<JobThread> running;

	JobThreadPool(int maxSize) {
		this.maxSize = maxSize;
		this.running = new HashSet<>(maxSize);
		this.available = new ArrayList<>(maxSize);
	}

	boolean isAvailable() {
		return available.size() > 0 || maxSize > running.size();
	}

	void submitJob(RunningJob job) {
		JobThread toRun = available.isEmpty() ? new JobThread() : pollAvailable();
		running.add(toRun);
		toRun.offerJob(() -> {
			job.run();
			running.remove(toRun);
			available.add(toRun);
		});
	}

	void gracefullyShutdown() {
		for(JobThread thread : running) {
			thread.gracefullyShutdown();
		}
		for(JobThread thread : available) {
			thread.gracefullyShutdown();
		}
	}

	private JobThread pollAvailable() {
		JobThread nextThread = available.get(0);
		available.remove(0);
		return nextThread;
	}

}
