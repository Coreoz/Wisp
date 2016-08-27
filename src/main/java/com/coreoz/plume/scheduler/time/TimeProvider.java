package com.coreoz.plume.scheduler.time;

public interface TimeProvider {

	/**
	 * Returns the current time in milliseconds
	 */
	long currentTime();

}
