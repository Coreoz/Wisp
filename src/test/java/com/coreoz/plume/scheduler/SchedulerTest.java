package com.coreoz.plume.scheduler;

import static org.fest.assertions.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coreoz.plume.scheduler.schedule.BasicSchedules;
import com.coreoz.plume.scheduler.stats.SchedulerStats;
import com.coreoz.plume.scheduler.time.SystemTimeProvider;

import lombok.SneakyThrows;

public class SchedulerTest {

	private static final Logger logger = LoggerFactory.getLogger(SchedulerTest.class);

	@Test
	public void should_run_a_single_job() throws InterruptedException {
		Scheduler scheduler = new Scheduler();
		SingleJob singleJob = new SingleJob();
		scheduler.schedule(
			"test",
			singleJob,
			BasicSchedules.executeOnce(BasicSchedules.fixedDurationSchedule(1))
		);

		waitOn(singleJob, () -> singleJob.countExecuted.get() > 0, 10000);

		scheduler.gracefullyShutdown();

		assertThat(singleJob.countExecuted.get()).isEqualTo(1);
	}

	@Test
	@SneakyThrows
	public void check_racing_conditions() {
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
			BasicSchedules.executeOnce(BasicSchedules.fixedDurationSchedule(1))
		);
		scheduler.schedule(
			"job2",
			job2,
			BasicSchedules.executeOnce(BasicSchedules.fixedDurationSchedule(1))
		);
		scheduler.schedule(
			"job3",
			job3,
			BasicSchedules.executeOnce(BasicSchedules.fixedDurationSchedule(1))
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
		long jobIntervalTime = 40L;
		Job job = scheduler.schedule(
			"job1",
			job1,
			BasicSchedules.executeOnce(BasicSchedules.fixedDurationSchedule(jobIntervalTime))
		);
		Thread thread1 = new Thread(() -> {
			waitOn(job1, () -> job1.countExecuted.get() > 0, 10000);
		});
		thread1.start();
		thread1.join();

		assertThat(job.lastExecutionTimeInMillis() - beforeExecutionTime)
			.isGreaterThanOrEqualTo(jobIntervalTime);
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
