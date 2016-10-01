package com.coreoz.plume.scheduler.schedule;

/**
 * Provide the time of the next executions of a job.
 * The implementations should be thread-safe.
 * Moreover the same instance of a schedule should be usable on multiple jobs.
 */
public interface Schedule {

	static final long WILL_NOT_BE_EXECUTED_AGAIN = -1L;

	/**
	 * Compute the next execution time for a job.
	 * This method should be thread-safe.
	 *
	 * @param executionsCount The number of times a job has already been executed
	 * @param currentTimeInMillis The current time in milliseconds. This time must be used if
	 * a next execution is planned for the job
	 * @return The time in milliseconds at which the job should execute next.
	 * This time must be relative to {@code currentTimeInMillis}.
	 * If the returned value is negative, the job will not be executed again.
	 */
	long nextExecutionInMillis(int executionsCount, long currentTimeInMillis);

}
