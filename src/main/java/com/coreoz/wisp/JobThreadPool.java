package com.coreoz.wisp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.coreoz.wisp.stats.ThreadPoolStats;

class JobThreadPool {

	private final Object lock;

	private final int maxSize;
	private final List<JobThread> available;
	private final Set<JobThread> running;

	// When a job is done, the thread pool still have some instructions to run.
	// That means there is a difference of perception between the scheduler
	// and the thread pool: for the scheduler a new thread is available,
	// whereas for the thread pool, the thread is still running.
	// This queue is used when the thread pool is full and the scheduler
	// think there is a slot available. The next job to run will be stacked
	// in this queue. Then after the thread executing the last job has finished
	// executing its last remaining instructions, it will execute the next job
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

	void submitJob(RunningJob job, boolean isSubmiterEndingJob) {
		// this enables to execute the next job on the same thread
		// that the job that has just finished executing.
		// => it enables to avoid creating an unneeded new thread
		if(isSubmiterEndingJob) {
			toRunAsap.add(job);
			return;
		}

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
