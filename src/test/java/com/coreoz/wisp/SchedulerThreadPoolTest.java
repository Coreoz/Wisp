package com.coreoz.wisp;

import static com.coreoz.wisp.Utils.waitOn;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.Test;

import com.coreoz.wisp.Utils.SingleJob;
import com.coreoz.wisp.schedule.Schedules;
import com.coreoz.wisp.stats.SchedulerStats;

public class SchedulerThreadPoolTest {

	@Test
	public void thread_pool_should_scale_down_when_no_more_tasks_need_executing() throws InterruptedException {
		Scheduler scheduler = new Scheduler(
			SchedulerConfig
				.builder()
				.threadsKeepAliveTime(Duration.ofMillis(50))
				.build()
		);

		runTwoConcurrentJobsForAtLeastFiftyIterations(scheduler);
		scheduler.cancel("job1");
		scheduler.cancel("job2");

		Thread.sleep(60L);
		SchedulerStats stats = scheduler.stats();
		scheduler.gracefullyShutdown();

		assertThat(
			stats.getThreadPoolStats().getActiveThreads()
			+ stats.getThreadPoolStats().getIdleThreads()
		)
		.isLessThanOrEqualTo(0);
		assertThat(stats.getThreadPoolStats().getLargestPoolSize()).isGreaterThanOrEqualTo(1);
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

		runTwoConcurrentJobsForAtLeastFiftyIterations(scheduler);

		SchedulerStats stats = scheduler.stats();
		scheduler.gracefullyShutdown();

		assertThat(
			stats.getThreadPoolStats().getActiveThreads()
			+ stats.getThreadPoolStats().getIdleThreads()
		)
		// 1 thread for each task => 2
		// but since most of the thread pool logic is delegated to ThreadPoolExecutor
		// we do not have precise control on how much threads will be created.
		// So we mostly want to check that not all threads of the pool are created.
		.isLessThanOrEqualTo(5);
	}

	@Test
	public void should_provide_accurate_pool_size_stats() throws InterruptedException {
		Scheduler scheduler = new Scheduler();
		scheduler.schedule(() -> {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				// do not care any exception
			}
			},
			Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ZERO))
		);

		Thread.sleep(30L);

		SchedulerStats stats = scheduler.stats();
		scheduler.gracefullyShutdown();

		assertThat(stats.getThreadPoolStats().getActiveThreads()).isEqualTo(1);
		assertThat(stats.getThreadPoolStats().getIdleThreads()).isEqualTo(0);
		assertThat(stats.getThreadPoolStats().getMinThreads()).isEqualTo(0);
		assertThat(stats.getThreadPoolStats().getMaxThreads()).isEqualTo(10);
		assertThat(stats.getThreadPoolStats().getLargestPoolSize()).isEqualTo(1);
	}

	private void runTwoConcurrentJobsForAtLeastFiftyIterations(Scheduler scheduler)
			throws InterruptedException {
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
	}

}
