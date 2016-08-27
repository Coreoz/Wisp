package com.coreoz.plume.scheduler.schedule;

import java.time.Duration;

public class BasicSchedules {

	public static Schedule fixedDurationSchedule(Duration duration) {
		return new FixedDurationSchedule(duration.getSeconds() * 1000);
	}

	public static Schedule fixedDurationSchedule(long intervalInMillis) {
		return new FixedDurationSchedule(intervalInMillis);
	}

	public static Schedule executeOnce(Schedule schedule) {
		return new OnceSchedule(schedule);
	}

	// TODO add basic API for a LocalDateTime
	// TODO add API for a schedule with the first execution after an initial delay
	// TODO add an API with cron expression

}
