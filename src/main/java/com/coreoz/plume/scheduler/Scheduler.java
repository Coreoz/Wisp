package com.coreoz.plume.scheduler;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coreoz.plume.scheduler.schedule.Schedule;
import com.coreoz.plume.scheduler.time.SystemTimeProvider;
import com.coreoz.plume.scheduler.time.TimeProvider;

/**
 * A job is executed only once at a time.
 * The scheduler will never execute the same job twice at a time.
 */
public class Scheduler {

	private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

	public static final int DEFAULT_THREAD_POOL_SIZE = 10;
	public static final long DEFAULT_MINIMUM_DELAY_TO_REPLACE_JOB = 10L;

	private final Jobs jobs;
	private final JobThreadPool threadPool;
	private final TimeProvider timeProvider;
	private final long minimumDelayInMillisToReplaceJob;

	private volatile boolean shuttingDown;

	public Scheduler() {
		this(DEFAULT_THREAD_POOL_SIZE);
	}

	public Scheduler(int nbThreads) {
		this(nbThreads, DEFAULT_MINIMUM_DELAY_TO_REPLACE_JOB);
	}

	public Scheduler(int nbThreads, long minimumDelayInMillisToReplaceJob) {
		this(nbThreads, minimumDelayInMillisToReplaceJob, new SystemTimeProvider());
	}

	public Scheduler(int nbThreads, long minimumDelayInMillisToReplaceJob,
			TimeProvider timeProvider) {
		this.jobs = new Jobs();
		this.minimumDelayInMillisToReplaceJob = minimumDelayInMillisToReplaceJob;
		this.threadPool = new JobThreadPool(nbThreads);
		this.timeProvider = timeProvider;

		this.shuttingDown = false;
	}

	public Job schedule(String nullableName, Runnable runnable, Schedule when) {
		// TODO check not null

		String name = nullableName == null ? runnable.toString() : nullableName;
		// TODO check job non déjà importé

		Job job = Job.of(JobStatus.DONE, 0, name, when, runnable);

		parkInPool(job);
		jobs.indexedByName().put(name, job);

		checkNextJobToRun();

		return job;
	}

	public Collection<Job> jobStatus() {
		return jobs.indexedByName().values();
	}

	public Optional<Job> findJob(String name) {
		return Optional.ofNullable(jobs.indexedByName().get(name));
	}

	public void gracefullyShutdown() {
		synchronized (jobs.nextExecutionsOrder()) {
			shuttingDown = true;

			if(jobs.nextRunningJob() != null) {
				tryCancelNextExecution();
			}

			threadPool.gracefullyShutdown();
		}
	}

	// package API

	void checkNextJobToRun() {
		synchronized (jobs.nextExecutionsOrder()) {
			if(jobs.nextExecutionsOrder().isEmpty() || shuttingDown) {
				// done :)
				return;
			}

			// if the next job to run will execute later than the next job in the queue
			// then the next job scheduled will be replaced by the next job in the queue
			Job nextJob = jobs.nextExecutionsOrder().first();
			if(jobs.nextRunningJob() != null
				&& jobs.nextRunningJob().job().status() == JobStatus.READY
				&& (
						jobs.nextRunningJob().job().nextExecutionTimeInMillis()
						+ minimumDelayInMillisToReplaceJob
					)
					> nextJob.nextExecutionTimeInMillis()
			) {
				tryCancelNextExecution();
				// the next job will be executed right after
				// the cancel job in returned to the pool
				return;
			}

			if(jobs.nextRunningJob() == null
				|| jobs.nextRunningJob().job().status() != JobStatus.READY) {
				runNextJob();
			}
		}
	}

	void parkInPool(Job executed) {
		logger.trace("park {} - running {}", executed, jobs.nextRunningJob());

		if(shuttingDown) {
			return;
		}

		if(jobs.nextRunningJob() != null && jobs.nextRunningJob().job() == executed) {
			jobs.nextRunningJob(null);
		}

		updateForNextExecution(executed);
		if(executed.status() == JobStatus.SCHEDULED) {
			synchronized (jobs.nextExecutionsOrder()) {
				jobs.nextExecutionsOrder().add(executed);
			}
			checkNextJobToRun();
		}
	}

	// internal

	private void tryCancelNextExecution() {
		jobs.nextRunningJob().shouldExecuteJob(false);
		synchronized (jobs.nextRunningJob().job()) {
			jobs.nextRunningJob().job().notifyAll();
		}
	}

	private void runNextJob() {
		if(threadPool.isAvailable()) {
			jobs.nextRunningJob(new RunningJob(
				jobs.nextExecutionsOrder().pollFirst(),
				this,
				timeProvider
			));
			threadPool.submitJob(jobs.nextRunningJob());
		} else {
			logger.warn("Job thread pool is full, either tasks take too much time to execute "
					+ "or either the thread pool is too small");
		}
	}

	private Job updateForNextExecution(Job job) {
		// if the job has not been executed, do not recalcule the next execution time
		if(job.nextExecutionTimeInMillis() < timeProvider.currentTime()) {
			job.nextExecutionTimeInMillis(job.schedule().nextExecutionInMillis(timeProvider));
		}

		if(job.nextExecutionTimeInMillis() > 0) {
			job.status(JobStatus.SCHEDULED);
		} else {
			job.status(JobStatus.DONE);
		}

		return job;
	}

}
