package com.coreoz.plume.scheduler.schedule;

import static org.fest.assertions.Assertions.assertThat;

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
	public void should_calcule_next_execution_from_midnight() {
		ZoneId ectZone = ZoneId.of("Europe/Paris");
		ZonedDateTime augustMidnight = LocalDate
			.of(2016, 8, 31)
			.atStartOfDay()
			.atZone(ectZone);
		long midDay = augustMidnight.toEpochSecond() * 1000;

		assertThat(new FixedHourSchedule("00:00:00").nextExecutionInMillis(0, midDay)).isEqualTo(0);
		assertThat(new FixedHourSchedule("00:00:01").nextExecutionInMillis(0, midDay)).isEqualTo(1000);
	}

	@Test
	public void should_calcule_next_execution_from_midday() {
		ZoneId ectZone = ZoneId.of("Europe/Paris");
		ZonedDateTime augustMidnight = LocalDate
			.of(2016, 8, 31)
			.atTime(12, 0)
			.atZone(ectZone);
		long midDay = augustMidnight.toEpochSecond() * 1000;

		assertThat(new FixedHourSchedule("12:00:00").nextExecutionInMillis(0, midDay)).isEqualTo(0);
		assertThat(new FixedHourSchedule("12:00:01").nextExecutionInMillis(0, midDay)).isEqualTo(1000);
		assertThat(new FixedHourSchedule("11:59:59").nextExecutionInMillis(0, midDay)).isEqualTo(24 * 60 * 60 * 1000 - 1000);
		assertThat(new FixedHourSchedule("00:00:00").nextExecutionInMillis(0, midDay)).isEqualTo(12 * 60 * 60 * 1000);
	}

	@Test
	public void should_calcule_next_execution_with_dst() {
		ZoneId ectZone = ZoneId.of("Europe/Paris");
		ZonedDateTime augustMidnight = LocalDate
			.of(2016, 10, 30)
			.atStartOfDay()
			.atZone(ectZone);
		long midDay = augustMidnight.toEpochSecond() * 1000;

		assertThat(new FixedHourSchedule("02:00:00", ectZone).nextExecutionInMillis(0, midDay)).isEqualTo(2 * 60 * 60 * 1000);
		assertThat(new FixedHourSchedule("03:00:00", ectZone).nextExecutionInMillis(0, midDay)).isEqualTo(4 * 60 * 60 * 1000);
	}

}
