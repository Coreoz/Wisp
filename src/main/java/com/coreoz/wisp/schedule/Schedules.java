package com.coreoz.wisp.schedule;

import java.time.Duration;

/**
 * Static helpers to build {@link Schedule}
 */
public class Schedules {

	public static Schedule fixedFrequencySchedule(Duration duration) {
		return new FixedDelaySchedule(duration);
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

}
