package com.coreoz.wisp.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import com.coreoz.wisp.Job;
import com.coreoz.wisp.JobStatus;
import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.SchedulerConfig;

public class FixedHourScheduleTest {

	@Test
	public void should_parse_time_without_seconds() {
		LocalTime executionTime = new FixedHourSchedule("12:31").executionTime();

		assertThat(executionTime.getHour()).isEqualTo(12);
		assertThat(executionTime.getMinute()).isEqualTo(31);
		assertThat(executionTime.getSecond()).isEqualTo(0);
	}

	@Test
	public void should_parse_time_with_seconds() {
		LocalTime executionTime = new FixedHourSchedule("03:31:09").executionTime();

		assertThat(executionTime.getHour()).isEqualTo(3);
		assertThat(executionTime.getMinute()).isEqualTo(31);
		assertThat(executionTime.getSecond()).isEqualTo(9);
	}

	@Test
	public void should_calcule_next_execution_from_epoch() {
		ZoneId ectZone = ZoneId.of("Europe/Paris");
		ZonedDateTime augustMidnight = LocalDate
			.of(2016, 8, 31)
			.atStartOfDay()
			.atZone(ectZone);
		long midnight = augustMidnight.toEpochSecond() * 1000;

		assertThat(
			new FixedHourSchedule("00:00:00", ectZone).nextExecutionInMillis(midnight, 0, null)
		).isEqualTo(midnight);
	}

	@Test
	public void should_calcule_next_execution_from_midnight() {
		ZoneId ectZone = ZoneId.of("Europe/Paris");
		ZonedDateTime augustMidnight = LocalDate
			.of(2016, 8, 31)
			.atStartOfDay()
			.atZone(ectZone);
		long midnight = augustMidnight.toEpochSecond() * 1000;

		assertThat(
			new FixedHourSchedule("00:00:00", ectZone).durationUntilNextExecutionInMillis(midnight, null)
		).isEqualTo(0);
		assertThat(
			new FixedHourSchedule("00:00:01", ectZone).durationUntilNextExecutionInMillis(midnight, null)
		).isEqualTo(1000);
	}

	@Test
	public void should_calcule_next_execution_from_midday() {
		ZoneId ectZone = ZoneId.of("Europe/Paris");
		ZonedDateTime augustMidday = LocalDate
			.of(2016, 8, 31)
			.atTime(12, 0)
			.atZone(ectZone);
		long midday = augustMidday.toEpochSecond() * 1000;

		assertThat(
			new FixedHourSchedule("12:00:00", ectZone).durationUntilNextExecutionInMillis(midday, null)
		).isEqualTo(0);
		assertThat(
			new FixedHourSchedule("12:00:01", ectZone).durationUntilNextExecutionInMillis(midday, null)
		).isEqualTo(1000);
		assertThat(
			new FixedHourSchedule("11:59:59", ectZone).durationUntilNextExecutionInMillis(midday, null)
		).isEqualTo(24 * 60 * 60 * 1000 - 1000);
		assertThat(
			new FixedHourSchedule("00:00:00", ectZone).durationUntilNextExecutionInMillis(midday, null)
		).isEqualTo(12 * 60 * 60 * 1000);
	}

	@Test
	public void should_calcule_next_execution_with_dst() {
		ZoneId ectZone = ZoneId.of("Europe/Paris");
		ZonedDateTime midnight = LocalDate
			.of(2016, 10, 30)
			.atStartOfDay()
			.atZone(ectZone);
		long midnightMillis = midnight.toEpochSecond() * 1000;

		assertThat(new FixedHourSchedule("02:00:00", ectZone).durationUntilNextExecutionInMillis(midnightMillis, null)).isEqualTo(2 * 60 * 60 * 1000);
		assertThat(new FixedHourSchedule("03:00:00", ectZone).durationUntilNextExecutionInMillis(midnightMillis, null)).isEqualTo(4 * 60 * 60 * 1000);
	}

	@Test
	public void should_calcule_next_execution_during_time_change() {
		ZoneId ectZone = ZoneId.of("Europe/Paris");
		ZonedDateTime oneSecBeforeTimeChange = LocalDate
			.of(2016, 10, 30)
			.atTime(1, 59, 59)
			.atZone(ectZone);
		long oneSecBeforeTimeChangeMillis = oneSecBeforeTimeChange.toEpochSecond() * 1000;
		long oneSecAfterTimeChangeMillis = (oneSecBeforeTimeChange.toEpochSecond() + 2) * 1000;

		assertThat(
			new FixedHourSchedule("02:00:00", ectZone)
				.durationUntilNextExecutionInMillis(oneSecBeforeTimeChangeMillis, null)
		).isEqualTo(1000);
		assertThat(
			new FixedHourSchedule("02:00:00", ectZone)
				.durationUntilNextExecutionInMillis(oneSecAfterTimeChangeMillis, null)
		).isEqualTo(25 * 60 * 60 * 1000 - 1000);
	}

	@Test
	public void should_not_return_current_time_if_last_execution_equals_current_time() {
		ZoneId ectZone = ZoneId.of("Europe/Paris");
		ZonedDateTime augustMidnight = LocalDate
			.of(2016, 8, 31)
			.atStartOfDay()
			.atZone(ectZone);
		long midnight = augustMidnight.toEpochSecond() * 1000;

		assertThat(
			new FixedHourSchedule("00:00:00", ectZone).durationUntilNextExecutionInMillis(midnight, midnight)
		).isEqualTo(24 * 60 * 60 * 1000);
	}

	@Test
	public void should_skip_past_execution_during_repeated_hour() {
		ZoneId zone = ZoneId.of("Europe/Paris");
		long currentTime = ZonedDateTime.parse("2016-10-30T02:15+01:00[Europe/Paris]").toInstant().toEpochMilli();
		long nextDay = ZonedDateTime.parse("2016-10-31T02:30+01:00[Europe/Paris]").toInstant().toEpochMilli();

		assertThat(new FixedHourSchedule("02:30", zone).nextExecutionInMillis(currentTime, 0, null))
			.isEqualTo(nextDay);
	}

	@Test
	public void should_wait_for_adjusted_execution_during_spring_gap() {
		ZoneId zone = ZoneId.of("Europe/Paris");
		long currentTime = ZonedDateTime.parse("2016-03-27T03:15+02:00[Europe/Paris]").toInstant().toEpochMilli();
		long executionTime = ZonedDateTime.parse("2016-03-27T03:30+02:00[Europe/Paris]").toInstant().toEpochMilli();
		long nextDay = ZonedDateTime.parse("2016-03-28T02:30+02:00[Europe/Paris]").toInstant().toEpochMilli();
		FixedHourSchedule schedule = new FixedHourSchedule("02:30", zone);

		assertThat(schedule.nextExecutionInMillis(currentTime, 0, null)).isEqualTo(executionTime);
		assertThat(schedule.nextExecutionInMillis(executionTime, 0, null)).isEqualTo(executionTime);
		assertThat(schedule.nextExecutionInMillis(executionTime, 1, executionTime)).isEqualTo(nextDay);
		assertThat(schedule.nextExecutionInMillis(executionTime + 1, 1, executionTime)).isEqualTo(nextDay);
	}

	@Test
	public void should_keep_daily_job_scheduled_during_repeated_hour() {
		long currentTime = ZonedDateTime.parse("2016-10-30T02:15+01:00[Europe/Paris]").toInstant().toEpochMilli();
		long nextDay = ZonedDateTime.parse("2016-10-31T02:30+01:00[Europe/Paris]").toInstant().toEpochMilli();
		try (Scheduler scheduler = new Scheduler(SchedulerConfig.builder().timeProvider(() -> currentTime).build())) {
			Job job = scheduler.schedule("daily", () -> {}, new FixedHourSchedule("02:30", ZoneId.of("Europe/Paris")));

			assertThat(job.status()).isEqualTo(JobStatus.SCHEDULED);
			assertThat(job.nextExecutionTimeInMillis()).isEqualTo(nextDay);
		}
	}

}
