package com.coreoz.wisp;

/**
 * Describe a {@link Job} state
 */
public enum JobStatus {

	/**
	 * The job will not be run ever again
	 */
	DONE,
	/**
	 * The job will be executed at his scheduled time
	 */
	SCHEDULED,
	/**
	 * This is an intermediate status before {@link #RUNNING},
	 * it means that the job is placed on the thread pool executor
	 * and is waiting to be executed as soon as a thread is available.
	 * This status should not last more than a few µs/ms except if the
	 * thread pool is full and running tasks are not terminating.
	 */
	READY,
	/**
	 * The job is currently running
	 */
	RUNNING,

}
