package com.coreoz.wisp.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

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

		assertThat(new FixedHourSchedule("00:00:00").nextExecutionInMillis(midnight, 0, null)).isEqualTo(midnight);
	}

	@Test
	public void should_calcule_next_execution_from_midnight() {
		ZoneId ectZone = ZoneId.of("Europe/Paris");
		ZonedDateTime augustMidnight = LocalDate
			.of(2016, 8, 31)
			.atStartOfDay()
			.atZone(ectZone);
		long midnight = augustMidnight.toEpochSecond() * 1000;

		assertThat(new FixedHourSchedule("00:00:00").durationUntilNextExecutionInMillis(midnight, null)).isEqualTo(0);
		assertThat(new FixedHourSchedule("00:00:01").durationUntilNextExecutionInMillis(midnight, null)).isEqualTo(1000);
	}

	@Test
	public void should_calcule_next_execution_from_midday() {
		ZoneId ectZone = ZoneId.of("Europe/Paris");
		ZonedDateTime augustMidday = LocalDate
			.of(2016, 8, 31)
			.atTime(12, 0)
			.atZone(ectZone);
		long midday = augustMidday.toEpochSecond() * 1000;

		assertThat(new FixedHourSchedule("12:00:00").durationUntilNextExecutionInMillis(midday, null)).isEqualTo(0);
		assertThat(new FixedHourSchedule("12:00:01").durationUntilNextExecutionInMillis(midday, null)).isEqualTo(1000);
		assertThat(new FixedHourSchedule("11:59:59").durationUntilNextExecutionInMillis(midday, null)).isEqualTo(24 * 60 * 60 * 1000 - 1000);
		assertThat(new FixedHourSchedule("00:00:00").durationUntilNextExecutionInMillis(midday, null)).isEqualTo(12 * 60 * 60 * 1000);
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

		assertThat(new FixedHourSchedule("02:00:00", ectZone).durationUntilNextExecutionInMillis(oneSecBeforeTimeChangeMillis, null)).isEqualTo(1000);
		assertThat(new FixedHourSchedule("02:00:00", ectZone).durationUntilNextExecutionInMillis(oneSecAfterTimeChangeMillis, null)).isEqualTo(25 * 60 * 60 * 1000 - 1000);
	}

	@Test
	public void should_not_return_current_time_if_last_execution_equals_current_time() {
		ZoneId ectZone = ZoneId.of("Europe/Paris");
		ZonedDateTime augustMidnight = LocalDate
			.of(2016, 8, 31)
			.atStartOfDay()
			.atZone(ectZone);
		long midnight = augustMidnight.toEpochSecond() * 1000;

		assertThat(new FixedHourSchedule("00:00:00").durationUntilNextExecutionInMillis(midnight, midnight)).isEqualTo(24 * 60 * 60 * 1000);
	}

}
