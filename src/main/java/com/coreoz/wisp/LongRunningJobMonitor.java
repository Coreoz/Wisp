package com.coreoz.wisp;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coreoz.wisp.time.TimeProvider;

import lombok.AllArgsConstructor;

/**
 * Detect jobs that are running for too long.
 * When a job is running for too long, a warning message is being logged.
 */
public class LongRunningJobMonitor implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(LongRunningJobMonitor.class);

	public static final Duration DEFAULT_THRESHOLD_DETECTION = Duration.ofMinutes(5);

	private final Scheduler scheduler;
	private final TimeProvider timeProvider;
	private final long detectionThresholdInMillis;
	private final Map<Job, LongRunningJobInfo> longRunningJobs;

	/**
	 * Create a new {@link LongRunningJobMonitor}.
	 * @param scheduler The scheduler that will be monitored
	 * @param detectionThreshold The threshold after which a message will be logged if the job takes longer to execute
	 * @param timeProvider The time provider used to calculate long running jobs
	 */
	public LongRunningJobMonitor(Scheduler scheduler, Duration detectionThreshold, TimeProvider timeProvider) {
		this.scheduler = scheduler;
		this.timeProvider = timeProvider;
		this.detectionThresholdInMillis = detectionThreshold.toMillis();

		this.longRunningJobs = new HashMap<>();
	}

	/**
	 * Create a new {@link LongRunningJobMonitor}.
	 * @param scheduler The scheduler that will be monitored
	 * @param detectionThreshold The threshold after which a message will be logged if the job takes longer to execute
	 */
	public LongRunningJobMonitor(Scheduler scheduler, Duration detectionThreshold) {
		this(scheduler, detectionThreshold, SchedulerConfig.DEFAULT_TIME_PROVIDER);
	}

	/**
	 * Create a new {@link LongRunningJobMonitor} with a 5 minutes detection threshold.
	 * @param scheduler The scheduler that will be monitored
	 */
	public LongRunningJobMonitor(Scheduler scheduler) {
		this(scheduler, DEFAULT_THRESHOLD_DETECTION, SchedulerConfig.DEFAULT_TIME_PROVIDER);
	}

	/**
	 * Run the too long jobs detection on the {@link Scheduler}.
	 * This method is *not* thread safe.
	 */
	@Override
	public void run() {
		long currentTime = timeProvider.currentTime();
		for(Job job : scheduler.jobStatus()) {
			cleanUpLongJobIfItHasFinishedExecuting(currentTime, job);
			detectLongRunningJob(currentTime, job);
		}
	}

	/**
	 * Check whether a job is running for too long or not.
	 *
	 * @return true if the is running for too long, else false.
	 * Returned value is made available for testing purposes.
	 */
	boolean detectLongRunningJob(long currentTime, Job job) {
		if(job.status() == JobStatus.RUNNING && !longRunningJobs.containsKey(job)) {
			int jobExecutionsCount = job.executionsCount();
			Long timeInMillisSinceJobRunning = job.timeInMillisSinceJobRunning();
			Thread threadRunningJob = job.threadRunningJob();

			if(timeInMillisSinceJobRunning != null
				&& threadRunningJob != null
				&& currentTime - timeInMillisSinceJobRunning > detectionThresholdInMillis) {
				logger.warn(
					"Job '{}' is still running after {}ms (detection threshold = {}ms), stack trace = {}",
					job.name(),
					currentTime - timeInMillisSinceJobRunning,
					detectionThresholdInMillis,
					Stream
						.of(threadRunningJob.getStackTrace())
						.map(StackTraceElement::toString)
						.collect(Collectors.joining("\n  "))
				);

				longRunningJobs.put(
					job,
					new LongRunningJobInfo(timeInMillisSinceJobRunning, jobExecutionsCount)
				);

				return true;
			}
		}
		return false;
	}

	/**
	 * cleanup jobs that have finished executing after {@link #thresholdDetectionInMillis}
	 */
	Long cleanUpLongJobIfItHasFinishedExecuting(long currentTime, Job job) {
		if(longRunningJobs.containsKey(job)
			&& longRunningJobs.get(job).executionsCount != job.executionsCount()) {
			Long jobLastExecutionTimeInMillis = job.lastExecutionTimeInMillis();
			int jobExecutionsCount = job.executionsCount();
			LongRunningJobInfo jobRunningInfo = longRunningJobs.get(job);

			long jobExecutionDuration = 0L;
			if(jobExecutionsCount == jobRunningInfo.executionsCount + 1
				&& jobLastExecutionTimeInMillis != null) {
				jobExecutionDuration = jobLastExecutionTimeInMillis - jobRunningInfo.timeInMillisSinceJobRunning;
				logger.info(
					"Job '{}' has finished executing after {}ms",
					job.name(),
					jobExecutionDuration
				);
			} else {
				jobExecutionDuration = currentTime - jobRunningInfo.timeInMillisSinceJobRunning;
				logger.info(
					"Job '{}' has finished executing after about {}ms",
					job.name(),
					jobExecutionDuration
				);
			}

			longRunningJobs.remove(job);
			return jobExecutionDuration;
		}

		return null;
	}

	@AllArgsConstructor
	private static class LongRunningJobInfo {
		final long timeInMillisSinceJobRunning;
		final int executionsCount;
	}

}
