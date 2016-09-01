package com.coreoz.plume.scheduler.schedule;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.coreoz.plume.scheduler.time.TimeProvider;

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
	public long nextExecutionInMillis(int executionsCount, TimeProvider timeProvider) {
		ZonedDateTime currentDateTime = Instant
										.ofEpochMilli(timeProvider.currentTime())
										.atZone(zoneId);

		return currentDateTime
				.until(nextExecutionDateTime(currentDateTime), ChronoUnit.MILLIS);
	}

	private ZonedDateTime nextExecutionDateTime(ZonedDateTime currentDateTime) {
		if(currentDateTime.toLocalTime().compareTo(executionTime) <= 0) {
			return executionTime.atDate(currentDateTime.toLocalDate()).atZone(zoneId);
		}
		return executionTime.atDate(currentDateTime.toLocalDate()).plusDays(1).atZone(zoneId);
	}

	@Override
	public String toString() {
		return "at " + executionTime + " " + zoneId;
	}

}
