package com.coreoz.wisp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coreoz.wisp.schedule.Schedules;
import com.coreoz.wisp.stats.SchedulerStats;
import com.coreoz.wisp.time.SystemTimeProvider;

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
		.isEqualTo(1);
	}

	@Test
	public void should_not_run_early_job() throws InterruptedException {
		SystemTimeProvider timeProvider = new SystemTimeProvider();
		Scheduler scheduler = new Scheduler(1, 10, timeProvider);
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
		for(int i=0; i<100; i++) {
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
	public void should_not_create_more_threads_than_necessary_on_startup__races_test() {
		for(int i=0; i<100; i++) {
			should_not_create_more_threads_than_necessary_on_startup();
		}
	}

	@Test
	public void should_not_create_more_threads_than_necessary_on_startup() {
		Scheduler scheduler = new Scheduler();

		scheduler.schedule("job1", () -> {}, Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ofSeconds(60))));
		scheduler.schedule("job2", () -> {}, Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ofSeconds(20))));

		SchedulerStats stats = scheduler.stats();
		scheduler.gracefullyShutdown();

		assertThat(
			stats.getThreadPoolStats().getActiveThreads()
			+ stats.getThreadPoolStats().getIdleThreads()
		)
		.isEqualTo(1);
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
		.isLessThanOrEqualTo(2);
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
		Scheduler scheduler = new Scheduler(1, 0);

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
