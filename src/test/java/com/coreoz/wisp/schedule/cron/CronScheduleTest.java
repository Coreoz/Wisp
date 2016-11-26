package com.coreoz.wisp.schedule.cron;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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

	@Test
	public void should_not_executed_daily_jobs_twice_a_day() {
		CronSchedule everyMinuteScheduler = CronSchedule.parseQuartzCron("0 0 12 * * ? *");

		ZonedDateTime augustMidday = LocalDate
			.of(2016, 8, 31)
			.atTime(12, 0)
			.atZone(ZoneId.systemDefault());
		long midday = augustMidday.toEpochSecond() * 1000;

		assertThat(everyMinuteScheduler.nextExecutionInMillis(midday-1, 0, null))
		.isEqualTo(midday);
		assertThat(everyMinuteScheduler.nextExecutionInMillis(midday, 0, null))
		.isEqualTo(midday + Duration.ofDays(1).toMillis());
	}

}
