package com.coreoz.plume.scheduler;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

import com.coreoz.plume.scheduler.schedule.BasicSchedules;

public class SchedulerTest {

	@Test
	public void should_run_a_single_job() throws InterruptedException {
		Scheduler scheduler = new Scheduler();
		SingleJob singleJob = new SingleJob();
		scheduler.schedule(
			"test",
			singleJob,
			BasicSchedules.executeOnce(BasicSchedules.fixedDurationSchedule(1))
		);
		synchronized (singleJob) {
			singleJob.wait(50);
		}
		scheduler.gracefullyShutdown();

		assertThat(singleJob.executed).isTrue();
	}

	@Test
	public void should_run_each_job_once() throws InterruptedException {
		Scheduler scheduler = new Scheduler(2);
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
			synchronized (job1) {
				try {
					job1.wait(50);
				} catch (InterruptedException e) {
				}
			}
		});
		thread1.start();
		Thread thread2 = new Thread(() -> {
			synchronized (job2) {
				try {
					job2.wait(50);
				} catch (InterruptedException e) {
				}
			}
		});
		thread2.start();
		Thread thread3 = new Thread(() -> {
			synchronized (job3) {
				try {
					job3.wait(50);
				} catch (InterruptedException e) {
				}
			}
		});
		thread3.start();

		thread1.join();
		thread2.join();
		thread3.join();

		scheduler.gracefullyShutdown();

		assertThat(job1.executed).isTrue();
		assertThat(job2.executed).isTrue();
		assertThat(job3.executed).isTrue();
	}

	private static class SingleJob implements Runnable {
		boolean executed = false;

		@Override
		public void run() {
			if(executed) {
				throw new RuntimeException("Job has already been executed");
			}
			executed = true;
			synchronized (this) {
				notifyAll();
			}
		}
	}

}
