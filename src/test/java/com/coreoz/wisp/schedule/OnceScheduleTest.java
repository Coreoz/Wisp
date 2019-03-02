package com.coreoz.wisp.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.Test;

import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.Utils;

public class OnceScheduleTest {

	@Test
	public void should_not_rely_only_on_job_executions_count() {
		Schedule onceAfter5ms = Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ofMillis(5)));

		assertThat(onceAfter5ms.nextExecutionInMillis(0, 2, null)).isEqualTo(5);
		assertThat(onceAfter5ms.nextExecutionInMillis(0, 2, null)).isEqualTo(5);
		assertThat(onceAfter5ms.nextExecutionInMillis(0, 3, null)).isEqualTo(Schedule.WILL_NOT_BE_EXECUTED_AGAIN);
	}

	@Test
	public void check_that_scheduler_really_execute_job_once() throws InterruptedException {
		Scheduler scheduler = new Scheduler();
		scheduler.schedule("job", Utils.doNothing(), Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ZERO)));

		Thread.sleep(100);
		scheduler.gracefullyShutdown();

		assertThat(scheduler.findJob("job").get().executionsCount()).isEqualTo(1);
	}

}
