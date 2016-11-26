package com.coreoz.wisp.schedule.cron;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.Test;

public class CronScheduleTest {

	@Test
	public void should_calcule_the_next_execution_time_based_on_a_unix_cron_expression() {
		CronSchedule everyMinuteScheduler = CronSchedule.parseUnixCron("* * * * *");

		assertThat(everyMinuteScheduler.nextExecutionInMillis(0, 0, null))
		.isEqualTo(Duration.ofMinutes(1).toMillis());
	}

	@Test
	public void should_calcule_the_next_execution_time_based_on_a_quartz_cron_expression() {
		CronSchedule everyMinuteScheduler = CronSchedule.parseQuartzCron("0 * * * * ? *");

		assertThat(everyMinuteScheduler.nextExecutionInMillis(0, 0, null))
		.isEqualTo(Duration.ofMinutes(1).toMillis());
	}

}
