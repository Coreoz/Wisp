package com.coreoz.wisp.schedule.cron;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

public class CronExpressionScheduleTest {

	@Test
	public void should_calcule_the_next_execution_time_based_on_a_unix_cron_expression() {
		CronExpressionSchedule everyMinuteScheduler = CronExpressionSchedule.parse("* * * * *");

		// To ease calculations, next execution time are calculated from the timestamp "0".
		// So here, the absolute timestamp for an execution in 1 minute will be 60
		assertThat(everyMinuteScheduler.nextExecutionInMillis(0, 0, null))
		.isEqualTo(Duration.ofMinutes(1).toMillis());
	}

	@Test
	public void should_calcule_the_next_execution_time_based_on_a_unix_cron_expression_with_seconds() {
		CronExpressionSchedule everyMinuteScheduler = CronExpressionSchedule.parseWithSeconds("29 * * * * *");

		assertThat(everyMinuteScheduler.nextExecutionInMillis(0, 0, null))
		// the first iteration will be the absolute timestamp "29"
		.isEqualTo(Duration.ofSeconds(29).toMillis());
		assertThat(everyMinuteScheduler.nextExecutionInMillis(29000 , 1, null))
		// the second iteration will be the absolute timestamp "89"
		.isEqualTo(Duration.ofSeconds(60 + 29).toMillis());
	}

	@Test
	public void should_not_executed_daily_jobs_twice_a_day() {
		CronExpressionSchedule everyMinuteScheduler = CronExpressionSchedule.parse("0 12 * * *");

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
