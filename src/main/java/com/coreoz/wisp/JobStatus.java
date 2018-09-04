package com.coreoz.wisp;

/**
 * Describe a {@link Job} state
 */
public enum JobStatus {

	/**
	 * will not be run ever again
	 */
	DONE,
	/**
	 * will be executed again, but not right now
	 */
	SCHEDULED,
	/**
	 * a scheduled job that is attached to a thread from the pool and ready to be executed when its time comes
	 */
	READY,
	/**
	 * a job currently running
	 */
	RUNNING,

}
