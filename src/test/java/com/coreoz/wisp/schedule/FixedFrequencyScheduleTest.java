package com.coreoz.wisp.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class FixedFrequencyScheduleTest {
	@Test
	public void test_next_execution_rounded() {
		assertThat(Schedules.fixedFrequencySchedule(Duration.ofHours(2))
				.nextExecutionInMillis(TimeUnit.SECONDS.toMillis(8), 0, 0L))
				.isEqualTo(TimeUnit.HOURS.toMillis(2));
	}

	@Test
	public void test_next_execution_too_much() {
		assertThat(Schedules.fixedFrequencySchedule(Duration.ofHours(2))
				.nextExecutionInMillis(TimeUnit.HOURS.toMillis(3), 0, 0L))
				.isEqualTo(TimeUnit.HOURS.toMillis(4));
	}

	@Test
	public void test_next_execution_not_rounded() {
		assertThat(Schedules.fixedFrequencySchedule(Duration.ofHours(2))
			.nextExecutionInMillis(TimeUnit.HOURS.toMillis(4), 0, 0L))
			.isEqualTo(TimeUnit.HOURS.toMillis(6));
	}
}
