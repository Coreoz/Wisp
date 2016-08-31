package com.coreoz.plume.scheduler.schedule;

import static org.fest.assertions.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

import org.junit.Test;

import com.coreoz.plume.scheduler.time.TimeProvider;

public class FixedHourScheduleTest {

	@Test
	public void should_parse_time_without_seconds() {
		OffsetTime executionTime = new FixedHourSchedule("12:31").executionTime();

		assertThat(executionTime.getHour()).isEqualTo(12);
		assertThat(executionTime.getMinute()).isEqualTo(31);
		assertThat(executionTime.getSecond()).isEqualTo(0);
	}

	@Test
	public void should_parse_time_with_seconds() {
		OffsetTime executionTime = new FixedHourSchedule("03:31:09").executionTime();

		assertThat(executionTime.getHour()).isEqualTo(3);
		assertThat(executionTime.getMinute()).isEqualTo(31);
		assertThat(executionTime.getSecond()).isEqualTo(9);
	}

	@Test
	public void should_calcule_next_execution() {
		OffsetDateTime augustMidnight = LocalDate
			.of(2016, 8, 31)
			.atStartOfDay()
			.atOffset(FixedHourSchedule.SYSTEM_OFFSET);
		TimeProvider midDay = () -> augustMidnight.toEpochSecond() * 1000;

		assertThat(new FixedHourSchedule("00:00:00").nextExecutionInMillis(0, midDay)).isEqualTo(0);
		assertThat(new FixedHourSchedule("00:00:01").nextExecutionInMillis(0, midDay)).isEqualTo(1000);
	}

	// TODO test time with jet lag

}
