package com.coreoz.plume.scheduler.schedule;

import java.time.Duration;

public class Schedules {

	public static Schedule fixedDurationSchedule(Duration duration) {
		return new FixedDurationSchedule(duration.getSeconds() * 1000);
	}

	public static Schedule fixedDurationSchedule(long intervalInMillis) {
		return new FixedDurationSchedule(intervalInMillis);
	}

	public static Schedule executeOnce(Schedule schedule) {
		return new OnceSchedule(schedule);
	}

	/**
	 * Execute a job at the same time every day.
	 * The file format must be "hh:mm" or "hh:mm:ss"
	 */
	public static Schedule executeAt(String time) {
		return new FixedHourSchedule(time);
	}

	// TODO add API for a schedule with the first execution after an initial delay
	// TODO add an API with cron expression (in CronSchedules)

}
