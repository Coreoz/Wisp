package com.coreoz.wisp.schedule;

/**
 * Provide the time of the next executions of a job.
 * The implementations should be thread-safe.
 * Moreover the same instance of a schedule should be usable on multiple jobs.
 */
public interface Schedule {

	/**
	 * If the {@link #nextExecutionInMillis(long, int, Long)} returned value is {@code -1},
	 * then the corresponding job will not be executed again.
	 */
	static final long WILL_NOT_BE_EXECUTED_AGAIN = -1L;
	/**
	 * A schedule that will always return {@link #WILL_NOT_BE_EXECUTED_AGAIN}.
	 */
	static final Schedule willNeverBeExecuted = (c, e, l) -> Schedule.WILL_NOT_BE_EXECUTED_AGAIN;

	/**
	 * Compute the next execution time for a job.
	 * This method should be thread-safe.
	 *
	 * @param currentTimeInMillis The current time in milliseconds. This time must be used if
	 * a next execution is planned for the job
	 * @param executionsCount The number of times a job has already been executed
	 * @param lastExecutionEndedTimeInMillis The time at which the job has last been executed; will be null
	 * if the job has never been executed
	 * @return The time in milliseconds at which the job should execute next.
	 * This time must be relative to {@code currentTimeInMillis}.
	 * If the returned value is negative, the job will not be executed again.
	 */
	long nextExecutionInMillis(long currentTimeInMillis, int executionsCount, Long lastExecutionEndedTimeInMillis);

}
