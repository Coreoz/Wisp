package com.coreoz.wisp.schedule;

import java.time.Duration;

/**
 * Static helpers to build {@link Schedule}
 */
public class Schedules {

	/**
	 * Execute a job at a fixed delay after each execution
	 */
	public static Schedule fixedDelaySchedule(Duration duration) {
		return new FixedDelaySchedule(duration);
	}

	/**
	 * Execute a job at the same time once a day.
	 * The time format must be "hh:mm" or "hh:mm:ss"
	 */
	public static Schedule executeAt(String time) {
		return new FixedHourSchedule(time);
	}

	// composition schedules

	public static Schedule executeOnce(Schedule schedule) {
		return new OnceSchedule(schedule);
	}

	public static Schedule afterInitialDelay(Schedule schedule, Duration initialDelay) {
		return new AfterInitialDelaySchedule(schedule, initialDelay);
	}

}
