package com.coreoz.plume.scheduler;

import java.lang.Thread.State;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JobThread {

	private static final Logger logger = LoggerFactory.getLogger(JobThread.class);

	private static final AtomicInteger threadCounter = new AtomicInteger(1);

	private final ThreadLoop threadLoop;
	private Thread thread;

	JobThread() {
		this.threadLoop = new ThreadLoop();
		this.thread = null;
	}

	void offerJob(Runnable job) {
		threadLoop.job = job;

		if(thread == null || thread.getState() == State.TERMINATED) {
			thread = new Thread(threadLoop, "Job Scheduler #" + threadCounter.getAndIncrement());
			thread.start();
		} else {
			synchronized (threadLoop) {
				threadLoop.notifyAll();
			}
		}
	}

	void gracefullyShutdown() {
		threadLoop.shuttingDown = true;
		synchronized (threadLoop) {
			threadLoop.notifyAll();
		}
		try {
			thread.join();
		} catch (InterruptedException e) {
			logger.trace("", e);
		}
	}

	private static class ThreadLoop implements Runnable {
		Runnable job = null;
		boolean shuttingDown = false;

		@Override
		public void run() {
			while(!shuttingDown) {
				try {
					if(job == null) {
						synchronized (this) {
							wait();
						}
					}
					if(job != null) {
						job.run();
						job = null;
					}
				} catch (InterruptedException e) {
					logger.error("", e);
				}
			}
		}

	}

}
