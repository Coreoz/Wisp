package com.coreoz.wisp;

import static com.coreoz.wisp.Utils.doNothing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.coreoz.wisp.Utils.SingleJob;
import com.coreoz.wisp.schedule.Schedules;

import lombok.Value;

/**
 * Tests about {@link Scheduler#cancel(String)} only
 */
public class SchedulerCancelTest {

	@Test
	public void cancel_should_throw_IllegalArgumentException_if_the_job_name_does_not_exist() {
		Scheduler scheduler = new Scheduler(SchedulerConfig.builder().maxThreads(1).build());
		try {
			scheduler.cancel("job that does not exist");
			fail("Should not accept to cancel a job that does not exist");
		} catch (IllegalArgumentException e) {
			// as expected :)
		}
		scheduler.gracefullyShutdown();
	}

	@Test
	public void cancel_should_returned_a_job_with_the_done_status() throws Exception {
		Scheduler scheduler = new Scheduler(SchedulerConfig.builder().maxThreads(1).build());
		scheduler.schedule("doNothing", doNothing(), Schedules.fixedDelaySchedule(Duration.ofMillis(100)));
		Job job = scheduler.cancel("doNothing").toCompletableFuture().get(1, TimeUnit.SECONDS);

		assertThat(job).isNotNull();
		assertThat(job.status()).isEqualTo(JobStatus.DONE);
		assertThat(scheduler.jobStatus().size()).isEqualTo(1);
		assertThat(job.name()).isEqualTo("doNothing");
		assertThat(job.runnable()).isSameAs(doNothing());

		scheduler.gracefullyShutdown();

		assertThat(job.executionsCount()).isEqualTo(0);
	}

	@Test
	public void second_cancel_should_return_either_the_first_promise_or_either_a_completed_future() throws Exception {
		Scheduler scheduler = new Scheduler(SchedulerConfig.builder().maxThreads(1).build());
		scheduler.schedule("job", Utils.TASK_THAT_SLEEPS_FOR_200MS, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));

		// so the job can start executing
		Thread.sleep(20L);

		CompletionStage<Job> cancelFuture = scheduler.cancel("job");
		CompletionStage<Job> otherCancelFuture = scheduler.cancel("job");
		assertThat(cancelFuture).isSameAs(otherCancelFuture);

		cancelFuture.toCompletableFuture().get(1, TimeUnit.SECONDS);

		CompletionStage<Job> lastCancelFuture = scheduler.cancel("job");
		assertThat(lastCancelFuture.toCompletableFuture().isDone()).isTrue();

		scheduler.gracefullyShutdown();
	}

	@Test
	public void cancelled_job_should_be_schedulable_again() throws Exception {
		Scheduler scheduler = new Scheduler(SchedulerConfig.builder().maxThreads(1).build());
		scheduler.schedule("doNothing", doNothing(), Schedules.fixedDelaySchedule(Duration.ofMillis(100)));
		scheduler.cancel("doNothing").toCompletableFuture().get(1, TimeUnit.SECONDS);

		Job job = scheduler.schedule("doNothing", doNothing(), Schedules.fixedDelaySchedule(Duration.ofMillis(100)));

		assertThat(job).isNotNull();
		assertThat(job.status()).isEqualTo(JobStatus.SCHEDULED);
		assertThat(job.name()).isEqualTo("doNothing");
		assertThat(job.runnable()).isSameAs(doNothing());

		scheduler.gracefullyShutdown();
	}

	@Test
	public void cancelling_a_job_should_wait_until_it_is_terminated_and_other_jobs_should_continue_running()
			throws InterruptedException, ExecutionException, TimeoutException {
		Scheduler scheduler = new Scheduler(SchedulerConfig.builder().maxThreads(1).build());

		SingleJob jobProcess1 = new SingleJob();
		SingleJob jobProcess2 = new SingleJob() {
			@Override
			public void run() {
				try {
					Thread.sleep(200);
					super.run();
				} catch (InterruptedException e) {
					throw new RuntimeException("Should not be interrupted", e);
				}
			}
		};

		Job job1 = scheduler.schedule(jobProcess1, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		Job job2 = scheduler.schedule(jobProcess2, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));

		Thread.sleep(20);

		int job1ExecutionsCount = job1.executionsCount();
		assertThat(job2.executionsCount()).isEqualTo(0);

		scheduler.cancel(job2.name()).toCompletableFuture().get(1, TimeUnit.SECONDS);

		Thread.sleep(60);
		scheduler.gracefullyShutdown();

		assertThat(job2.executionsCount()).isEqualTo(1);
		assertThat(jobProcess2.countExecuted.get()).isEqualTo(1);
		// after job 2 is cancelled, job 1 should have been executed at least 3 times
		assertThat(job1.executionsCount()).isGreaterThan(job1ExecutionsCount + 3);
	}

	@Test
	public void a_job_should_be_cancelled_immediatly_if_it_has_the_status_ready() throws InterruptedException, ExecutionException, TimeoutException {
		Scheduler scheduler = new Scheduler(SchedulerConfig.builder().maxThreads(1).build());

		SingleJob jobProcess1 = new SingleJob();
		SingleJob jobProcess2 = new SingleJob() {
			@Override
			public void run() {
				try {
					Thread.sleep(300);
					super.run();
				} catch (InterruptedException e) {
					throw new RuntimeException("Should not be interrupted", e);
				}
			}
		};

		Job job1 = scheduler.schedule("Job 1", jobProcess1, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		scheduler.schedule("Job 2", jobProcess2, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));

		Thread.sleep(30);
		assertThat(job1.status()).isEqualTo(JobStatus.READY);

		long timeBeforeCancel = System.currentTimeMillis();
		scheduler.cancel(job1.name()).toCompletableFuture().get(1, TimeUnit.SECONDS);
		assertThat(timeBeforeCancel - System.currentTimeMillis()).isLessThan(50L);

		scheduler.gracefullyShutdown();
	}

	@Test
	public void cancelling_a_job_should_wait_until_it_is_terminated_and_other_jobs_should_continue_running__races_test()
			throws Exception {
		for(int i=0; i<10; i++) {
			cancelling_a_job_should_wait_until_it_is_terminated_and_other_jobs_should_continue_running();
		}
	}


	@Test
	public void scheduling_a_done_job_should_keep_its_previous_stats() throws InterruptedException, ExecutionException, TimeoutException {
		Scheduler scheduler = new Scheduler();

		Job job = scheduler.schedule("job", doNothing(), Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		Thread.sleep(25L);

		scheduler.cancel("job").toCompletableFuture().get(1, TimeUnit.SECONDS);
		Job newJob = scheduler.schedule("job", doNothing(), Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		scheduler.gracefullyShutdown();

		assertThat(newJob.executionsCount()).isGreaterThanOrEqualTo(job.executionsCount());
		assertThat(newJob.lastExecutionEndedTimeInMillis()).isNotNull();
	}

	@Test
	public void check_that_a_done_job_scheduled_again_keeps_its_scheduler_stats() throws InterruptedException, ExecutionException, TimeoutException {
		Scheduler scheduler = new Scheduler();

		Job job = scheduler.schedule("job", doNothing(), Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		Thread.sleep(25L);

		scheduler.cancel("job").toCompletableFuture().get(1, TimeUnit.SECONDS);
		int beforeScheduledAgainCount = job.executionsCount();
		assertThat(beforeScheduledAgainCount).as("First job must have enough time to execute").isGreaterThan(0);

		Queue<ScheduledExecution> scheduledExecutions = new ConcurrentLinkedQueue<>();
		scheduler.schedule("job", doNothing(), (long currentTimeInMillis, int executionsCount, Long lastExecutionEndedTimeInMillis) -> {
			scheduledExecutions.add(ScheduledExecution.of(currentTimeInMillis, executionsCount, lastExecutionEndedTimeInMillis));
			return currentTimeInMillis;
		});
		Thread.sleep(25L);
		scheduler.gracefullyShutdown();

		for(ScheduledExecution scheduledExecution : scheduledExecutions) {
			assertThat(scheduledExecution.executionsCount).isGreaterThanOrEqualTo(beforeScheduledAgainCount);
			assertThat(scheduledExecution.lastExecutionEndedTimeInMillis).isNotNull();
			assertThat(scheduledExecution.lastExecutionEndedTimeInMillis).isLessThanOrEqualTo(scheduledExecution.currentTimeInMillis);
		}
	}

	@Value(staticConstructor = "of")
	private static final class ScheduledExecution {
		private long currentTimeInMillis;
		private int executionsCount;
		private Long lastExecutionEndedTimeInMillis;
	}

}
