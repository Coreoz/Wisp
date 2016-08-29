package com.coreoz.plume.scheduler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JobThread {

	private static final Logger logger = LoggerFactory.getLogger(JobThread.class);

	private static final AtomicInteger threadCounter = new AtomicInteger(1);

	private final ThreadLoop threadLoop;
	private final BlockingQueue<Runnable> toRun;
	private final Thread thread;

	JobThread() {
		this.toRun = new LinkedBlockingQueue<>();
		this.threadLoop = new ThreadLoop(toRun);
		this.thread = new Thread(threadLoop, "Plume Scheduler Worker #" + threadCounter.getAndIncrement());
		this.thread.start();
	}

	void offerJob(Runnable job) {
		toRun.offer(job);
	}

	void gracefullyShutdown() {
		threadLoop.shuttingDown = true;
		toRun.offer(() -> {});
	}

	private static class ThreadLoop implements Runnable {
		private final BlockingQueue<Runnable> toRun;

		volatile boolean shuttingDown;

		public ThreadLoop(BlockingQueue<Runnable> toRun) {
			this.toRun = toRun;
			this.shuttingDown = false;
		}

		@Override
		public void run() {
			while(!shuttingDown) {
				try {
					toRun.take().run();
				} catch (InterruptedException e) {
					logger.error("", e);
				}
			}
		}
	}

}
