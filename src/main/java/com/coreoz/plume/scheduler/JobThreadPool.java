package com.coreoz.plume.scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.coreoz.plume.scheduler.stats.ThreadPoolStats;

class JobThreadPool {

	private final Object lock;

	private final int maxSize;
	private final List<JobThread> available;
	private final Set<JobThread> running;

	// TODO explain this
	private final BlockingQueue<RunningJob> toRunAsap;

	JobThreadPool(int maxSize) {
		this.lock = new Object();
		this.maxSize = maxSize;
		this.running = new HashSet<>(maxSize);
		this.available = new ArrayList<>(maxSize);
		this.toRunAsap = new LinkedBlockingQueue<>();
	}

	ThreadPoolStats stats() {
		synchronized (lock) {
			return ThreadPoolStats.of(running.size(), available.size());
		}
	}

	void submitJob(RunningJob job) {
		JobThread toRun = toRun(job);

		if(toRun != null) {
			toRun.offerJob(() -> {
				RunningJob toExecute = job;
				while(toExecute != null) {
					toExecute.run();
					synchronized (lock) {
						toExecute = toRunAsap.poll();
						if(toExecute == null) {
							running.remove(toRun);
							available.add(toRun);
						}
					}
				}
			});
		}
	}

	void gracefullyShutdown() {
		List<JobThread> toShutdown = new ArrayList<>(maxSize);
		synchronized (lock) {
			toShutdown.addAll(running);
			toShutdown.addAll(available);
		}
		for(JobThread thread : toShutdown) {
			thread.gracefullyShutdown();
		}
	}

	private JobThread toRun(RunningJob job) {
		JobThread toRun = null;
		synchronized (lock) {
			if(available.isEmpty()) {
				if(maxSize == running.size()) {
					toRunAsap.offer(job);
				} else {
					toRun = new JobThread();
					running.add(toRun);
				}
			} else {
				toRun = available.remove(0);
				running.add(toRun);
			}
		}
		return toRun;
	}

}
