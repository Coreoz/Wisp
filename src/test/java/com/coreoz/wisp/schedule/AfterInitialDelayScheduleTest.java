package com.coreoz.wisp.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.Test;

import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.Utils;

public class AfterInitialDelayScheduleTest {

	@Test
	public void first_execution_should_depends_only_on_the_first_delay() {
		Schedule after1msDelay = Schedules.afterInitialDelay(null, Duration.ofMillis(1));

		assertThat(after1msDelay.nextExecutionInMillis(0, 0, null)).isEqualTo(1);
	}

	@Test
	public void should_not_rely_only_on_job_executions_count() {
		Schedule every5ms = Schedules.fixedDelaySchedule(Duration.ofMillis(5));
		Schedule afterUnusedDelay = Schedules.afterInitialDelay(every5ms, Duration.ZERO);

		assertThat(afterUnusedDelay.nextExecutionInMillis(0, 2, null)).isEqualTo(0);
		assertThat(afterUnusedDelay.nextExecutionInMillis(0, 2, null)).isEqualTo(0);
		assertThat(afterUnusedDelay.nextExecutionInMillis(0, 3, null)).isEqualTo(5);
	}

	@Test
	public void check_that_scheduler_really_rely_on_initial_delay() throws InterruptedException {
		Scheduler scheduler = new Scheduler();
		scheduler.schedule("job", Utils.doNothing(), Schedules.afterInitialDelay(Schedules.fixedDelaySchedule(Duration.ofDays(1)), Duration.ZERO));

		Thread.sleep(100);
		scheduler.gracefullyShutdown();

		assertThat(scheduler.findJob("job").get().executionsCount()).isEqualTo(1);
	}

}
