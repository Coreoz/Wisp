package com.coreoz.wisp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.Test;

public class LongRunningJobMonitorTest {

	@Test
	public void detectLongRunningJob__check_running_job_limits_detection() {
		LongRunningJobMonitor detector = new LongRunningJobMonitor(null, Duration.ofMillis(10));

		Job job = newJob();
		job.lastExecutionStartedTimeInMillis(-11L);
		job.threadRunningJob(Thread.currentThread());
		assertThat(detector.detectLongRunningJob(0, job)).isTrue();

		job = newJob();
		job.lastExecutionStartedTimeInMillis(-12L);
		job.threadRunningJob(Thread.currentThread());
		assertThat(detector.detectLongRunningJob(0, job)).isTrue();

		job = newJob();
		job.lastExecutionStartedTimeInMillis(-10L);
		job.threadRunningJob(Thread.currentThread());
		assertThat(detector.detectLongRunningJob(0, job)).isFalse();

		job = newJob();
		job.lastExecutionStartedTimeInMillis(-9L);
		job.threadRunningJob(Thread.currentThread());
		assertThat(detector.detectLongRunningJob(0, job)).isFalse();
	}

	@Test
	public void detectLongRunningJob__check_that_a_job_long_execution_is_detected_only_once() {
		LongRunningJobMonitor detector = new LongRunningJobMonitor(null, Duration.ofMillis(10));

		Job job = newJob();
		job.lastExecutionStartedTimeInMillis(-100L);
		job.threadRunningJob(Thread.currentThread());

		assertThat(detector.detectLongRunningJob(0, job)).isTrue();
		assertThat(detector.detectLongRunningJob(0, job)).isFalse();
	}

	@Test
	public void detectLongRunningJob__check_that_a_job_with_a_null_running_time_or_thread_is_not_detected() {
		LongRunningJobMonitor detector = new LongRunningJobMonitor(null, Duration.ofMillis(10));

		Job job = newJob();
		assertThat(detector.detectLongRunningJob(0, job)).isFalse();

		job.lastExecutionStartedTimeInMillis(-100L);
		assertThat(detector.detectLongRunningJob(0, job)).isFalse();

		job.lastExecutionStartedTimeInMillis(null);
		job.threadRunningJob(Thread.currentThread());
		assertThat(detector.detectLongRunningJob(0, job)).isFalse();
	}

	@Test
	public void detectLongRunningJob__check_that_a_job_not_being_run_is_not_detected_as_too_long() {
		LongRunningJobMonitor detector = new LongRunningJobMonitor(null);
		Job job = newJob();
		job.status(JobStatus.SCHEDULED);
		job.lastExecutionStartedTimeInMillis(0L); // running for a long time...
		job.threadRunningJob(Thread.currentThread());

		assertThat(detector.detectLongRunningJob(System.currentTimeMillis(), job)).isFalse();
		job.status(JobStatus.DONE);
		assertThat(detector.detectLongRunningJob(System.currentTimeMillis(), job)).isFalse();
		job.status(JobStatus.READY);
		assertThat(detector.detectLongRunningJob(System.currentTimeMillis(), job)).isFalse();
	}

	@Test
	public void cleanUpLongJobIfItHasFinishedExecuting__check_that_a_job_not_detected_is_not_cleaned() {
		LongRunningJobMonitor detector = new LongRunningJobMonitor(null);
		Job job = newJob();
		assertThat(detector.cleanUpLongJobIfItHasFinishedExecuting(0, job)).isNull();
	}

	@Test
	public void cleanUpLongJobIfItHasFinishedExecuting__check_that_a_detected_same_running_job_is_not_cleaned() {
		LongRunningJobMonitor detector = new LongRunningJobMonitor(null, Duration.ofMillis(10));

		Job job = newJob();
		job.lastExecutionStartedTimeInMillis(-100L);
		job.threadRunningJob(Thread.currentThread());
		detector.detectLongRunningJob(0, job);

		assertThat(detector.cleanUpLongJobIfItHasFinishedExecuting(0, job)).isNull();
	}

	@Test
	public void cleanUpLongJobIfItHasFinishedExecuting__check_that_the_exact_job_execution_time_is_logged_when_job_execution_is_incremented_by_one() {
		LongRunningJobMonitor detector = new LongRunningJobMonitor(null, Duration.ofMillis(10));

		Job job = newJob();
		job.lastExecutionStartedTimeInMillis(-100L);
		job.threadRunningJob(Thread.currentThread());
		detector.detectLongRunningJob(0, job);
		job.executionsCount(1);
		job.lastExecutionEndedTimeInMillis(-50L);

		assertThat(detector.cleanUpLongJobIfItHasFinishedExecuting(0, job)).isEqualTo(50L);
	}

	@Test
	public void cleanUpLongJobIfItHasFinishedExecuting__check_that_the_approximate_job_execution_time_is_logged_when_job_execution_is_incremented_by_more_than_one() {
		LongRunningJobMonitor detector = new LongRunningJobMonitor(null, Duration.ofMillis(10));

		Job job = newJob();
		job.lastExecutionStartedTimeInMillis(-100L);
		job.threadRunningJob(Thread.currentThread());
		detector.detectLongRunningJob(0, job);
		job.executionsCount(2);
		job.lastExecutionEndedTimeInMillis(-50L);

		assertThat(detector.cleanUpLongJobIfItHasFinishedExecuting(0, job)).isEqualTo(100L);
	}

	@Test
	public void cleanUpLongJobIfItHasFinishedExecuting__check_that_a_clean_job_execution_is_really_cleaned() {
		LongRunningJobMonitor detector = new LongRunningJobMonitor(null, Duration.ofMillis(10));

		Job job = newJob();
		job.lastExecutionStartedTimeInMillis(-100L);
		job.threadRunningJob(Thread.currentThread());
		detector.detectLongRunningJob(0, job);
		job.executionsCount(1);
		job.lastExecutionEndedTimeInMillis(-50L);

		assertThat(detector.cleanUpLongJobIfItHasFinishedExecuting(0, job)).isEqualTo(50L);
		assertThat(detector.cleanUpLongJobIfItHasFinishedExecuting(0, job)).isNull();
	}

	private Job newJob() {
		return new Job(
			JobStatus.RUNNING,
			-1L,
			0,
			null,
			null,
			"job name",
			null,
			null
		);
	}

}
