package com.coreoz.wisp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coreoz.wisp.schedule.Schedules;
import com.coreoz.wisp.stats.SchedulerStats;
import com.coreoz.wisp.time.SystemTimeProvider;
import com.google.common.util.concurrent.Runnables;

public class SchedulerTest {

	private static final Logger logger = LoggerFactory.getLogger(SchedulerTest.class);

	@Test
	public void should_run_a_single_job() throws InterruptedException {
		Scheduler scheduler = new Scheduler();
		SingleJob singleJob = new SingleJob();
		scheduler.schedule(
			"test",
			singleJob,
			Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ofMillis(1)))
		);

		waitOn(singleJob, () -> singleJob.countExecuted.get() > 0, 10000);

		scheduler.gracefullyShutdown();

		assertThat(singleJob.countExecuted.get()).isEqualTo(1);
	}

	@Test
	public void check_racing_conditions() throws InterruptedException {
		for(int i = 1; i <= 10000; i++) {
			should_run_each_job_once();
			logger.info("iteration {} done", i);
		}
		for(int i = 1; i <= 100; i++) {
			should_not_run_early_job();
			logger.info("iteration {} done", i);
		}
	}

	@Test
	public void should_run_each_job_once() throws InterruptedException {
		Scheduler scheduler = new Scheduler(1);
		SingleJob job1 = new SingleJob();
		SingleJob job2 = new SingleJob();
		SingleJob job3 = new SingleJob();
		scheduler.schedule(
			"job1",
			job1,
			Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ofMillis(1)))
		);
		scheduler.schedule(
			"job2",
			job2,
			Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ofMillis(1)))
		);
		scheduler.schedule(
			"job3",
			job3,
			Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ofMillis(1)))
		);
		Thread thread1 = new Thread(() -> {
			waitOn(job1, () -> job1.countExecuted.get() > 0, 10000);
		});
		thread1.start();
		Thread thread2 = new Thread(() -> {
			waitOn(job2, () -> job2.countExecuted.get() > 0, 10000);
		});
		thread2.start();
		Thread thread3 = new Thread(() -> {
			waitOn(job3, () -> job3.countExecuted.get() > 0, 10000);
		});
		thread3.start();

		thread1.join();
		thread2.join();
		thread3.join();

		SchedulerStats stats = scheduler.stats();
		scheduler.gracefullyShutdown();

		assertThat(job1.countExecuted.get()).isEqualTo(1);
		assertThat(job2.countExecuted.get()).isEqualTo(1);
		assertThat(job3.countExecuted.get()).isEqualTo(1);

		assertThat(
			stats.getThreadPoolStats().getActiveThreads()
			+ stats.getThreadPoolStats().getIdleThreads()
		)
		// 1 thread for the launcher + 1 thread for the tasks
		.isLessThanOrEqualTo(2);
	}

	@Test
	public void should_not_run_early_job() throws InterruptedException {
		SystemTimeProvider timeProvider = new SystemTimeProvider();
		Scheduler scheduler = new Scheduler(SchedulerConfig
			.builder()
			.maxThreads(1)
			.timeProvider(timeProvider)
			.build()
		);
		SingleJob job1 = new SingleJob();
		long beforeExecutionTime = timeProvider.currentTime();
		Duration jobIntervalTime = Duration.ofMillis(40);

		Job job = scheduler.schedule(
			"job1",
			job1,
			Schedules.executeOnce(Schedules.fixedDelaySchedule(jobIntervalTime))
		);
		Thread thread1 = new Thread(() -> {
			waitOn(job1, () -> job1.countExecuted.get() > 0, 10000);
		});
		thread1.start();
		thread1.join();
		scheduler.gracefullyShutdown();

		assertThat(job.lastExecutionTimeInMillis() - beforeExecutionTime)
			.isGreaterThanOrEqualTo(jobIntervalTime.toMillis());
	}

	@Test
	public void should_not_execute_past_job() throws InterruptedException {
		Scheduler scheduler = new Scheduler();
		SingleJob job1 = new SingleJob();

		scheduler.schedule(
			"job1",
			job1,
			Schedules.fixedDelaySchedule(Duration.ofMillis(-1000))
		);
		Thread thread1 = new Thread(() -> {
			waitOn(job1, () -> job1.countExecuted.get() > 0, 500);
		});
		thread1.start();
		thread1.join();
		scheduler.gracefullyShutdown();

		assertThat(job1.countExecuted.get()).isEqualTo(0);
	}

	@Test
	public void should_shutdown_instantly_if_no_job_is_running__races_test() {
		for(int i=0; i<10000; i++) {
			should_shutdown_instantly_if_no_job_is_running();
		}
	}

	@Test
	public void should_shutdown_instantly_if_no_job_is_running() {
		Scheduler scheduler = new Scheduler();

		scheduler.schedule("job1", () -> {}, Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ofSeconds(60))));
		scheduler.schedule("job2", () -> {}, Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ofSeconds(20))));

		long beforeShutdownTime = System.currentTimeMillis();
		scheduler.gracefullyShutdown();

		assertThat(System.currentTimeMillis() - beforeShutdownTime).isLessThan(Duration.ofSeconds(5).toMillis());
	}

	@Test
	public void should_not_create_more_threads_than_jobs_scheduled_over_time__races_test()
			throws InterruptedException {
		for(int i=0; i<100; i++) {
			should_not_create_more_threads_than_jobs_scheduled_over_time();
		}
	}

	@Test
	public void should_not_create_more_threads_than_jobs_scheduled_over_time() throws InterruptedException {
		Scheduler scheduler = new Scheduler();

		SingleJob job1 = new SingleJob();
		SingleJob job2 = new SingleJob();

		scheduler.schedule("job1", job1, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		scheduler.schedule("job2", job2, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));

		Thread thread1 = new Thread(() -> {
			waitOn(job1, () -> job1.countExecuted.get() > 50, 100);
		});
		thread1.start();
		thread1.join();
		Thread thread2 = new Thread(() -> {
			waitOn(job2, () -> job2.countExecuted.get() > 50, 100);
		});
		thread2.start();
		thread2.join();

		SchedulerStats stats = scheduler.stats();
		scheduler.gracefullyShutdown();

		assertThat(
			stats.getThreadPoolStats().getActiveThreads()
			+ stats.getThreadPoolStats().getIdleThreads()
		)
		// 1 thread for the launcher + 1 thread for each task
		.isLessThanOrEqualTo(3);
	}

	@Test
	public void exception_in_schedule_should_not_alter_scheduler__races_test()
			throws InterruptedException {
		for(int i=0; i<1000; i++) {
			exception_in_schedule_should_not_alter_scheduler();
		}
	}

	@Test
	public void exception_in_schedule_should_not_alter_scheduler() throws InterruptedException {
		Scheduler scheduler = new Scheduler(SchedulerConfig.builder().maxThreads(1).build());

		AtomicBoolean isJob1ExecutedAfterJob2 = new AtomicBoolean(false);
		SingleJob job2 = new SingleJob();
		SingleJob job1 = new SingleJob() {
			@Override
			public void run() {
				super.run();
				if(job2.countExecuted.get() > 0) {
					isJob1ExecutedAfterJob2.set(true);
				}
			}
		};

		scheduler.schedule("job1", job1, Schedules.fixedDelaySchedule(Duration.ofMillis(3)));
		scheduler.schedule("job2", job2, (currentTimeInMillis, executionsCount, lastExecutionTimeInMillis) -> {
			if(executionsCount == 0) {
				return currentTimeInMillis;
			}
			throw new RuntimeException();
		});

		Thread thread1 = new Thread(() -> {
			waitOn(job1, () -> isJob1ExecutedAfterJob2.get(), 1000);
		});
		thread1.start();
		thread1.join();
		Thread thread2 = new Thread(() -> {
			waitOn(job2, () -> job2.countExecuted.get() > 0, 1000);
		});
		thread2.start();
		thread2.join();

		scheduler.gracefullyShutdown();

		assertThat(isJob1ExecutedAfterJob2.get()).isTrue();
	}

	// cancellation checks

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
		scheduler.schedule("doNothing", Runnables.doNothing(), Schedules.fixedDelaySchedule(Duration.ofMillis(100)));
		Job job = scheduler.cancel("doNothing").toCompletableFuture().get(1, TimeUnit.SECONDS);

		assertThat(job).isNotNull();
		assertThat(job.status()).isEqualTo(JobStatus.DONE);
		assertThat(scheduler.jobStatus().isEmpty()).isTrue();
		assertThat(job.name()).isEqualTo("doNothing");
		assertThat(job.runnable()).isSameAs(Runnables.doNothing());

		scheduler.gracefullyShutdown();

		assertThat(job.executionsCount()).isEqualTo(0);
	}

	@Test
	public void cancelled_job_should_be_schedulable_again() throws Exception {
		Scheduler scheduler = new Scheduler(SchedulerConfig.builder().maxThreads(1).build());
		scheduler.schedule("doNothing", Runnables.doNothing(), Schedules.fixedDelaySchedule(Duration.ofMillis(100)));
		scheduler.cancel("doNothing").toCompletableFuture().get(1, TimeUnit.SECONDS);

		Job job = scheduler.schedule("doNothing", Runnables.doNothing(), Schedules.fixedDelaySchedule(Duration.ofMillis(100)));

		assertThat(job).isNotNull();
		assertThat(job.status()).isEqualTo(JobStatus.SCHEDULED);
		assertThat(job.name()).isEqualTo("doNothing");
		assertThat(job.runnable()).isSameAs(Runnables.doNothing());

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
					Thread.sleep(100);
					super.run();
				} catch (InterruptedException e) {
					logger.error("Should not be interrupted", e);
				}
			}
		};

		Job job1 = scheduler.schedule(jobProcess1, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		Job job2 = scheduler.schedule(jobProcess2, Schedules.afterInitialDelay(
			Schedules.fixedDelaySchedule(Duration.ofMillis(1)),
			Duration.ofMillis(5)
		));

		Thread.sleep(10);

		int job1ExecutionsCount = job1.executionsCount();
		assertThat(job1ExecutionsCount).isGreaterThan(0);
		assertThat(job2.executionsCount()).isEqualTo(0);

		scheduler.cancel(job2.name()).toCompletableFuture().get(1, TimeUnit.SECONDS);

		Thread.sleep(30);
		scheduler.gracefullyShutdown();

		assertThat(job2.executionsCount()).isEqualTo(1);
		assertThat(jobProcess2.countExecuted.get()).isEqualTo(1);
		// after job 2 is cancelled, job 1 should have been executed at least 5 times
		assertThat(job1.executionsCount()).isGreaterThan(job1ExecutionsCount + 5);
	}

	@Test
	public void cancelling_a_job_should_wait_until_it_is_terminated_and_other_jobs_should_continue_running__races_test()
			throws Exception {
		for(int i=0; i<5; i++) {
			cancelling_a_job_should_wait_until_it_is_terminated_and_other_jobs_should_continue_running();
		}
	}

	private static class SingleJob implements Runnable {
		AtomicInteger countExecuted = new AtomicInteger(0);

		@Override
		public void run() {
			countExecuted.incrementAndGet();
			synchronized (this) {
				notifyAll();
			}
		}
	}

	private static void waitOn(Object lockOn, Supplier<Boolean> condition, long maxWait) {
		long currentTime = System.currentTimeMillis();
		long waitUntil = currentTime + maxWait;
		while(!condition.get() && waitUntil > currentTime) {
			synchronized (lockOn) {
				try {
					lockOn.wait(5);
				} catch (InterruptedException e) {
				}
			}
			currentTime = System.currentTimeMillis();
		}
	}

}
