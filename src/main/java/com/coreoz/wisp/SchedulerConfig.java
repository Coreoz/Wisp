package com.coreoz.wisp;

import java.time.Duration;

import com.coreoz.wisp.time.SystemTimeProvider;
import com.coreoz.wisp.time.TimeProvider;

import lombok.Builder;
import lombok.Value;

/**
 * The configuration used by the scheduler
 */
@Value
@Builder
public class SchedulerConfig {

	private static final TimeProvider DEFAULT_TIME_PROVIDER = new SystemTimeProvider();
	private static final Duration NON_EXPIRABLE_THREADS = Duration.ofMillis(Long.MAX_VALUE);

	/**
	 * The minimum number of threads that will live in the jobs threads pool.
	 */
	@Builder.Default private final int minThreads = 0;
	/**
	 * The maximum number of threads that will live in the jobs threads pool.
	 * Note that the limit of threads will actually be maxThreads + 1,
	 * because one thread is reserved for the scheduler internals.
	 */
	@Builder.Default private final int maxThreads = 10;
	/**
	 * If the number of created threads in greater than {@link #minThreads},
	 * the time after which idle threads will be removed from the threads pool.
	 */
	@Builder.Default private final Duration threadsKeepAliveTime = NON_EXPIRABLE_THREADS;
	/**
	 * The time provider that will be used by the scheduler
	 */
	@Builder.Default private final TimeProvider timeProvider = DEFAULT_TIME_PROVIDER;

}
