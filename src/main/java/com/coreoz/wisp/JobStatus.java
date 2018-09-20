package com.coreoz.wisp;

/**
 * Describe a {@link Job} state
 */
public enum JobStatus {

	/**
	 * the job will not be run ever again
	 */
	DONE,
	/**
	 * the job will be executed
	 */
	SCHEDULED,
	/**
	 * a scheduled job that will probably run sooner than another scheduled job :)
	 * @deprecated This status is not used anymore. It will be deleted in version 2.0.0.
	 */
	@Deprecated
	READY,
	/**
	 * the job is currently running
	 */
	RUNNING,

}
