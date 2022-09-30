package com.coreoz.wisp;

import static com.coreoz.wisp.Utils.waitOn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.assertj.core.data.Offset;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coreoz.wisp.Utils.SingleJob;
import com.coreoz.wisp.schedule.Schedules;
import com.coreoz.wisp.stats.SchedulerStats;
import com.coreoz.wisp.time.SystemTimeProvider;

public class SchedulerTest {

	private static final Logger logger = LoggerFactory.getLogger(SchedulerTest.class);

	@Test
	public void check_that_two_job_cannot_be_scheduled_with_the_same_name() {
		Scheduler scheduler = new Scheduler();

		scheduler.schedule("job", Utils.doNothing(), Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		try {
			scheduler.schedule("job", Utils.doNothing(), Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
			fail();
		} catch (IllegalArgumentException e) {
			// as expected
		}

		scheduler.gracefullyShutdown();
	}

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
			should_not_launch_job_early();
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

		// ensure that the initial thread limit is not exceeded
		assertThat(
			stats.getThreadPoolStats().getActiveThreads()
			+ stats.getThreadPoolStats().getIdleThreads()
		)
		.isLessThanOrEqualTo(1);
	}

	@Test
	public void should_not_launch_job_early() throws InterruptedException {
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

		assertThat(job.lastExecutionEndedTimeInMillis() - beforeExecutionTime)
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
			throw new RuntimeException("Expected exception");
		});

		Thread thread1 = new Thread(() -> {
			waitOn(job1, () -> isJob1ExecutedAfterJob2.get(), 10000);
		});
		thread1.start();
		thread1.join();
		Thread thread2 = new Thread(() -> {
			waitOn(job2, () -> job2.countExecuted.get() > 0, 10000);
		});
		thread2.start();
		thread2.join();

		scheduler.gracefullyShutdown();

		assertThat(isJob1ExecutedAfterJob2.get()).isTrue();
	}

	@Test
	public void exception_in_job_should_not_prevent_the_job_from_being_executed_again() throws InterruptedException {
		Scheduler scheduler = new Scheduler();

		Runnable runnable = () -> { throw new RuntimeException("Excepted exception"); };

		Job job = scheduler.schedule(
			runnable,
			Schedules.afterInitialDelay(
				Schedules.fixedDelaySchedule(Duration.ofMillis(5)),
				Duration.ZERO
			)
		);
		Thread.sleep(150L);
		scheduler.gracefullyShutdown();

		assertThat(job.executionsCount()).isGreaterThan(1);
	}

	@Test
	public void check_that_a_scheduled_job_has_the_right_status() {
		Scheduler scheduler = new Scheduler();

		Job job = scheduler.schedule(Utils.doNothing(), Schedules.fixedDelaySchedule(Duration.ofSeconds(1)));

		assertThat(job.status()).isEqualTo(JobStatus.SCHEDULED);

		scheduler.gracefullyShutdown();
		assertThat(job.status()).isEqualTo(JobStatus.DONE);
	}

	@Test
	public void check_that_a_running_job_has_the_right_status() throws InterruptedException {
		Scheduler scheduler = new Scheduler();

		Job job = scheduler.schedule(Utils.TASK_THAT_SLEEPS_FOR_200MS, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		Thread.sleep(40L);
		assertThat(job.status()).isEqualTo(JobStatus.RUNNING);
		scheduler.gracefullyShutdown();
		assertThat(job.status()).isEqualTo(JobStatus.DONE);
	}

	@Test
	public void check_that_a_long_running_job_does_not_prevent_other_jobs_to_run() throws InterruptedException {
		Scheduler scheduler = new Scheduler();

		Job job = scheduler.schedule(Utils.doNothing(), Schedules.fixedDelaySchedule(Duration.ofMillis(10)));
		Thread.sleep(25L);
		scheduler.schedule(Utils.TASK_THAT_SLEEPS_FOR_200MS, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		long countBeforeSleep = job.executionsCount();
		Thread.sleep(50L);
		scheduler.gracefullyShutdown();

		assertThat(job.executionsCount() - countBeforeSleep).isGreaterThan(3);
	}

	@Test
	public void check_that_metrics_are_correctly_updated_during_and_after_a_job_execution() throws InterruptedException {
		Scheduler scheduler = new Scheduler();

		Job job = scheduler.schedule(Utils.TASK_THAT_SLEEPS_FOR_200MS, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));

		Thread.sleep(25L);
		assertThat(job.executionsCount()).isZero();
		assertThat(job.lastExecutionEndedTimeInMillis()).isNull();
		assertThat(job.lastExecutionStartedTimeInMillis()).isCloseTo(System.currentTimeMillis(), Offset.offset(200L));
		assertThat(job.threadRunningJob()).isNotNull();

		scheduler.gracefullyShutdown();

		assertThat(job.executionsCount()).isOne();
		assertThat(job.lastExecutionEndedTimeInMillis()).isNotNull();
		assertThat(job.lastExecutionStartedTimeInMillis()).isNotNull();
		assertThat(job.threadRunningJob()).isNull();
	}

}
