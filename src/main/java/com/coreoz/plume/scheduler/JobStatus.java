package com.coreoz.plume.scheduler;

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
	 * a scheduled job that will probably run sooner than another scheduled job :)
	 */
	READY,
	/**
	 * a job currently running
	 */
	RUNNING,

}
