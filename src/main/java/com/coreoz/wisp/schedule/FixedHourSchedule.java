package com.coreoz.wisp.schedule;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class FixedHourSchedule implements Schedule {

	private final LocalTime executionTime;
	private final ZoneId zoneId;

	/**
	 * Parse time in the form of "hh:mm" or "hh:mm:ss"
	 */
	public FixedHourSchedule(String every) {
		this(LocalTime.parse(every));
	}

	/**
	 * Parse time in the form of "hh:mm" or "hh:mm:ss"
	 */
	public FixedHourSchedule(String every, ZoneId zoneId) {
		this(LocalTime.parse(every), zoneId);
	}

	public FixedHourSchedule(LocalTime every) {
		this(every, ZoneOffset.systemDefault());
	}

	public FixedHourSchedule(LocalTime every, ZoneId zoneId) {
		this.executionTime = every;
		this.zoneId = zoneId;
	}

	public LocalTime executionTime() {
		return executionTime;
	}

	public ZoneId zoneId() {
		return zoneId;
	}

	@Override
	public long nextExecutionInMillis(long currentTimeInMillis, int executionsCount, Long lastExecutionTimeInMillis) {
		return durationUntilNextExecutionInMillis(currentTimeInMillis, lastExecutionTimeInMillis)
				+ currentTimeInMillis;
	}

	long durationUntilNextExecutionInMillis(long currentTimeInMillis, Long lastExecutionTimeInMillis) {
		ZonedDateTime currentDateTime = Instant
				.ofEpochMilli(currentTimeInMillis)
				.atZone(zoneId);

		return currentDateTime
			.until(
				nextExecutionDateTime(
					currentDateTime,
					lastExecutionTimeInMillis != null && lastExecutionTimeInMillis == currentTimeInMillis
				),
				ChronoUnit.MILLIS
			);
	}

	private ZonedDateTime nextExecutionDateTime(ZonedDateTime currentDateTime, boolean nextExecutionShouldBeNextDay) {
		if(!nextExecutionShouldBeNextDay && currentDateTime.toLocalTime().compareTo(executionTime) <= 0) {
			return executionTime.atDate(currentDateTime.toLocalDate()).atZone(zoneId);
		}
		return executionTime.atDate(currentDateTime.toLocalDate()).plusDays(1).atZone(zoneId);
	}

	@Override
	public String toString() {
		return "at " + executionTime + " " + zoneId;
	}

}
