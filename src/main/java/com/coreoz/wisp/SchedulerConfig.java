package com.coreoz.wisp;

import java.time.Duration;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import com.coreoz.wisp.time.SystemTimeProvider;
import com.coreoz.wisp.time.TimeProvider;

import lombok.Builder;
import lombok.Getter;

/**
 * The configuration used by the scheduler
 */
@Getter
@Builder
public class SchedulerConfig {
	public static final TimeProvider DEFAULT_TIME_PROVIDER = new SystemTimeProvider();
	private static final Duration NON_EXPIRABLE_THREADS = Duration.ofMillis(Long.MAX_VALUE);

	/**
	 * The minimum number of threads that will live in the jobs threads pool.
	 */
	@Builder.Default private final int minThreads = 0;
	/**
	 * The maximum number of threads that will live in the jobs threads pool.
	 */
	@Builder.Default private final int maxThreads = 10;
	/**
	 * The time after which idle threads will be removed from the threads pool.
	 * By default the thread pool does not scale down (duration = infinity ~ {@link Long#MAX_VALUE}ms)
	 */
	@Builder.Default private final Duration threadsKeepAliveTime = NON_EXPIRABLE_THREADS;
	/**
	 * The time provider that will be used by the scheduler
	 */
	@Builder.Default private final TimeProvider timeProvider = DEFAULT_TIME_PROVIDER;
	/**
	 * The thread factory used to create worker threads on which jobs will be executed
	 */
	@Builder.Default private final Supplier<ThreadFactory> threadFactory = WispThreadFactory::new;
}
