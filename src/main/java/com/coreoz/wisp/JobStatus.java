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
	 * @deprecated This status is not used anymore. It will be deleted in version 2.0.0.
	 */
	@Deprecated
	READY,
	/**
	 * The job is currently running
	 */
	RUNNING,

}
