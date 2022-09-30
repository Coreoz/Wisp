package com.coreoz.wisp.time;

/**
 * The time provider that will be used by the scheduler to plan jobs
 */
public interface TimeProvider {

	/**
	 * Returns the current time in milliseconds
	 */
	long currentTime();

}
