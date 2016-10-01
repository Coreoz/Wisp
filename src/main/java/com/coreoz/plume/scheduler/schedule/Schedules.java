package com.coreoz.plume.scheduler.schedule;

import java.time.Duration;

/**
 * Static helpers to build {@link Schedule}
 */
public class Schedules {

	public static Schedule fixedDurationSchedule(Duration duration) {
		return new FixedDurationSchedule(duration);
	}

	public static Schedule executeOnce(Schedule schedule) {
		return new OnceSchedule(schedule);
	}

	public static Schedule afterInitialDelay(Schedule schedule, Duration initialDelay) {
		return new AfterInitialDelaySchedule(schedule, initialDelay);
	}

	/**
	 * Execute a job at the same time every day.
	 * The file format must be "hh:mm" or "hh:mm:ss"
	 */
	public static Schedule executeAt(String time) {
		return new FixedHourSchedule(time);
	}

	// TODO add an API with cron expression (in CronSchedules)

}
