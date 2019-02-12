package com.coreoz.wisp;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coreoz.wisp.time.TimeProvider;

/**
 * Detect jobs that are running for too long.
 */
public class LongRunningJobMonitor implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(LongRunningJobMonitor.class);

	public static final Duration DEFAULT_THRESHOLD_DETECTION = Duration.ofMinutes(5);

	private final Scheduler scheduler;
	private final TimeProvider timeProvider;
	private final long thresholdDetectionInMillis;

	public LongRunningJobMonitor(Scheduler scheduler, TimeProvider timeProvider, Duration thresholdDetection) {
		this.scheduler = scheduler;
		this.timeProvider = timeProvider;
		this.thresholdDetectionInMillis = thresholdDetection.toMillis();
	}

	public LongRunningJobMonitor(Scheduler scheduler) {
		this(scheduler, SchedulerConfig.DEFAULT_TIME_PROVIDER, DEFAULT_THRESHOLD_DETECTION);
	}

	@Override
	public void run() {
		long currentTime = timeProvider.currentTime();
		for(Job job : scheduler.jobStatus()) {
			if(job.status() == JobStatus.RUNNING
				&& currentTime - job.timeInMillisSinceWhenJobRunning() > thresholdDetectionInMillis) {
				// TODO check that variables are not null since no lock are being set on the job execution
				// TODO log a message
				// TODO save that a message has been logged for the current execution
			}
		}
		// TODO log a message telling that the job finished executing and the execution time
		// => execution time is job.lastExecutionTime - stored timeInMillisSinceWhenJobRunning
		// if stored executionsCount + 1 = job.executionsCount
		// else provide an estimation
	}

}
