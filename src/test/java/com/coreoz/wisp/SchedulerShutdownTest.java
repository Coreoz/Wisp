package com.coreoz.wisp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.Test;

import com.coreoz.wisp.Utils.SingleJob;
import com.coreoz.wisp.schedule.Schedules;

public class SchedulerShutdownTest {

	@Test
	public void shutdown_should_be_immediate_if_no_job_is_running() {
		Scheduler scheduler = new Scheduler();
		long beforeShutdown = System.currentTimeMillis();

		scheduler.gracefullyShutdown();

		assertThat(System.currentTimeMillis() - beforeShutdown).isLessThan(20L);
	}

	@Test
	public void second_shutdown_should_still_wait_for_its_timeout() throws InterruptedException {
		Scheduler scheduler = new Scheduler();

		scheduler.schedule(Utils.TASK_THAT_SLEEP_FOR_200MS, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		// so the job can start executing
		Thread.sleep(20L);

		try {
			scheduler.gracefullyShutdown(Duration.ofMillis(20)); // this will throw the exception
			throw new InterruptedException(); // so the compiler is happy
		} catch (InterruptedException e) {
			// as excepted
		}
		long beforeSecondShutdown = System.currentTimeMillis();
		scheduler.gracefullyShutdown();

		assertThat(System.currentTimeMillis() - beforeSecondShutdown).isGreaterThan(100L);
	}

	@Test
	public void ready_job_should_finish_without_being_executed_during_shutdown() throws InterruptedException {
		Scheduler scheduler = new Scheduler(SchedulerConfig.builder().maxThreads(1).build());

		scheduler.schedule(Utils.TASK_THAT_SLEEP_FOR_200MS, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		// so the job can start executing
		Thread.sleep(20L);
		SingleJob jobThatExecuteInTwoseconds = new SingleJob() {
			@Override
			public void run() {
				try {
					Thread.sleep(2000);
					super.run();
				} catch (InterruptedException e) {
					throw new RuntimeException("Should not be interrupted", e);
				}
			}
		};
		Job jobThatWillNotBeExecuted = scheduler.schedule(jobThatExecuteInTwoseconds, Schedules.fixedDelaySchedule(Duration.ofMillis(1)));
		// so the job can be ready to be executed
		Thread.sleep(20L);

		assertThat(jobThatWillNotBeExecuted.status()).isEqualTo(JobStatus.READY);
		long beforeShutdown = System.currentTimeMillis();
		scheduler.gracefullyShutdown();
		assertThat(System.currentTimeMillis() - beforeShutdown).isLessThan(200L);
		assertThat(jobThatWillNotBeExecuted.executionsCount()).isZero();
	}

}
