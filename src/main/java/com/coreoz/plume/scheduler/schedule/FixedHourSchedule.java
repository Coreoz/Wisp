package com.coreoz.plume.scheduler.schedule;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import com.coreoz.plume.scheduler.time.TimeProvider;

public class FixedHourSchedule implements Schedule {

	static final ZoneOffset SYSTEM_OFFSET = LocalDateTime
														.now()
														.atZone(ZoneOffset.systemDefault())
														.getOffset();

	private final OffsetTime executionTime;

	/**
	 * Parse time in the form of "hh:mm" or "hh:mm:ss"
	 */
	public FixedHourSchedule(String every) {
		this(LocalTime.parse(every));
	}

	public FixedHourSchedule(LocalTime every) {
		this(every.atOffset(SYSTEM_OFFSET));
	}

	public FixedHourSchedule(OffsetTime executionTime) {
		this.executionTime = executionTime;
	}

	public OffsetTime executionTime() {
		return executionTime;
	}

	@Override
	public long nextExecutionInMillis(int executionsCount, TimeProvider timeProvider) {
		OffsetDateTime currentDateTime = Instant
										.ofEpochMilli(timeProvider.currentTime())
										.atOffset(SYSTEM_OFFSET);

		return currentDateTime
				.until(nextExecutionDateTime(currentDateTime), ChronoUnit.MILLIS);
	}

	private OffsetDateTime nextExecutionDateTime(OffsetDateTime currentDateTime) {
		if(currentDateTime.toOffsetTime().compareTo(executionTime) <= 0) {
			return executionTime.atDate(currentDateTime.toLocalDate());
		}
		return executionTime.atDate(currentDateTime.toLocalDate().plusDays(1));
	}

	@Override
	public String toString() {
		return "at " + executionTime;
	}

}
