package com.coreoz.wisp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coreoz.wisp.schedule.Schedule;
import com.coreoz.wisp.stats.SchedulerStats;
import com.coreoz.wisp.stats.ThreadPoolStats;
import com.coreoz.wisp.time.SystemTimeProvider;
import com.coreoz.wisp.time.TimeProvider;

import lombok.SneakyThrows;

/**
 * A {@code Scheduler} instance reference a group of jobs
 * and is responsible to schedule these jobs at the expected time.<br/>
 * <br/>
 * A job is executed only once at a time.
 * The scheduler will never execute the same job twice at a time.
 */
public final class Scheduler {

	private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

	/**
	 * @deprecated Default values are available in {@link SchedulerConfig}
	 * It will be deleted in version 2.0.0.
	 */
	@Deprecated
	public static final int DEFAULT_THREAD_POOL_SIZE = 10;
	/**
	 * @deprecated This value is not used anymore
	 * It will be deleted in version 2.0.0.
	 */
	@Deprecated
	public static final long DEFAULT_MINIMUM_DELAY_IN_MILLIS_TO_REPLACE_JOB = 10L;

	private final ThreadPoolExecutor threadPoolExecutor;
	private final TimeProvider timeProvider;
	private final Object launcherNotifier;

	// jobs
	private final Map<String, Job> indexedJobsByName;
	private final ArrayList<Job> nextExecutionsOrder;

	private volatile boolean shuttingDown;

	// constructors

	public Scheduler() {
		this(SchedulerConfig.builder().build());
	}

	public Scheduler(int maxThreads) {
		this(SchedulerConfig.builder().maxThreads(maxThreads).build());
	}

	public Scheduler(SchedulerConfig config) {
		// TODO: validate conf

		this.indexedJobsByName = new ConcurrentHashMap<>();
		this.nextExecutionsOrder = new ArrayList<>();
		this.timeProvider = config.getTimeProvider();
		this.launcherNotifier = new Object();
		// TODO "Wisp Scheduler Worker #" + threadCounter.getAndIncrement());
		this.threadPoolExecutor = new ThreadPoolExecutor(
			config.getMinThreads(),
			// +1 is to include the launcher thread
			config.getMaxThreads() + 1,
			config.getThreadsKeepAliveTime().toMillis(),
			TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>()
		);
		if(config.getMinThreads() > 0) {
			this.threadPoolExecutor.prestartAllCoreThreads();
		}
	}

	/**
	 * @deprecated Use {@link #Scheduler(SchedulerConfig)} to specify multiple configuration values.
	 * It will be deleted in version 2.0.0.
	 */
	@Deprecated
	public Scheduler(int nbThreads, long minimumDelayInMillisToReplaceJob) {
		this(nbThreads, minimumDelayInMillisToReplaceJob, new SystemTimeProvider());
	}

	/**
	 * @deprecated Use {@link #Scheduler(SchedulerConfig)} to specify multiple configuration values.
	 * It will be deleted in version 2.0.0.
	 */
	@Deprecated
	public Scheduler(int nbThreads, long minimumDelayInMillisToReplaceJob,
			TimeProvider timeProvider) {
		this(SchedulerConfig
			.builder()
			.maxThreads(nbThreads)
			.timeProvider(timeProvider)
			.build()
		);
	}

	// public API

	/**
	 * Schedule the executions of a process.
	 *
	 * @param runnable The process to be executed at a schedule
	 * @param when The {@link Schedule} at which the process will be executed
	 * @return The corresponding {@link Job} created.
	 */
	public Job schedule(Runnable runnable, Schedule when) {
		return schedule(null, runnable, when);
	}

	/**
	 * Schedule the executions of a process.
	 *
	 * @param nullableName The name of the created job
	 * @param runnable The process to be executed at a schedule
	 * @param when The {@link Schedule} at which the process will be executed
	 * @return The corresponding {@link Job} created.
	 */
	public Job schedule(String nullableName, Runnable runnable, Schedule when) {
		Objects.requireNonNull(runnable, "Runnable must not be null");
		Objects.requireNonNull(when, "Schedule must not be null");

		String name = nullableName == null ? runnable.toString() : nullableName;
		Job job = new Job(JobStatus.DONE, 0L, 0, null, name, when, runnable);

		long currentTimeInMillis = timeProvider.currentTime();
		if(when.nextExecutionInMillis(currentTimeInMillis, 0, null) < currentTimeInMillis) {
			logger.warn("The job '{}' is scheduled at a paste date: it will never be executed", name);
		}

		// lock needed to make sure 2 jobs with the same name are not submitted at the same time
		synchronized (indexedJobsByName) {
			if(findJob(name).isPresent()) {
				throw new IllegalArgumentException("A job is already scheduled with the name:" + name);
			}
			indexedJobsByName.put(name, job);
		}

		logger.info("Scheduling job '{}' to run {}", job.name(), job.schedule());
		scheduleNextExecution(job);

		return job;
	}

	/**
	 * Fetch the status of all the jobs that has been registered on the {@code Scheduler}
	 */
	public Collection<Job> jobStatus() {
		return indexedJobsByName.values();
	}

	/**
	 * Find a job by its name
	 */
	public Optional<Job> findJob(String name) {
		return Optional.ofNullable(indexedJobsByName.get(name));
	}

	/**
	 * Issue a cancellation order for a job and
	 * returns immediately a promise that enables to follow the job cancellation status<br>
	 * <br>
	 * If the job is running, the scheduler will wait until it is finished to remove it
	 * from the jobs pool.
	 * If the job is not running, the job will just be removed from the pool.<br>
	 * After the job is cancelled, the job has the status {@link JobStatus#DONE}.
	 *
	 * @param jobName The job name to cancel
	 * @return The promise that succeed when the job is correctly cancelled
	 * and will not be executed again. If the job is running when {@link #cancel(String)}
	 * is called, the promise will succeed when the job has finished executing.
	 * @throws IllegalArgumentException if there is no job corresponding to the job name.
	 */
	public CompletionStage<Job> cancel(String jobName) {
		// TODO to implement :)
		return CompletableFuture.completedFuture(indexedJobsByName.get(jobName));
	}

	/**
	 * Wait until the current running jobs are executed
	 * and cancel jobs that are planned to be executed.
	 * There is a 10 seconds timeout
	 * @throws InterruptedException if the shutdown lasts more than 10 seconds
	 */
	public void gracefullyShutdown() {
		gracefullyShutdown(Duration.ofSeconds(10));
	}

	/**
	 * Wait until the current running jobs are executed
	 * and cancel jobs that are planned to be executed.
	 * @param timeout The maximum time to wait
	 * @throws InterruptedException if the shutdown lasts more than 10 seconds
	 */
	@SneakyThrows
	public void gracefullyShutdown(Duration timeout) {
		if(shuttingDown) {
			return;
		}

		logger.info("Shutting down...");

		synchronized (this) {
			shuttingDown = true;
			synchronized (launcherNotifier) {
				launcherNotifier.notify();
			}
		}

		threadPoolExecutor.shutdown();
		threadPoolExecutor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
	}

	/**
	 * Fetch statistics about the current {@code Scheduler}
	 */
	public SchedulerStats stats() {
		int activeThreads = threadPoolExecutor.getActiveCount();
		return SchedulerStats.of(ThreadPoolStats.of(
			activeThreads,
			threadPoolExecutor.getPoolSize() - activeThreads
		));
	}

	// internal

	private void scheduleNextExecution(Job job) {
		long currentTimeInMillis = timeProvider.currentTime();

		try {
			job.nextExecutionTimeInMillis(
				job.schedule().nextExecutionInMillis(
					currentTimeInMillis, job.executionsCount(), job.lastExecutionTimeInMillis()
				)
			);
		} catch (Throwable t) {
			logger.error(
				"An exception was raised during the job next execution time calculation,"
				+ " therefore the job {} will not be executed again.",
				job.name(),
				t
			);
			job.nextExecutionTimeInMillis(Schedule.WILL_NOT_BE_EXECUTED_AGAIN);
		}

		if(job.nextExecutionTimeInMillis() >= currentTimeInMillis) {
			job.status(JobStatus.SCHEDULED);
			synchronized (this) {
				nextExecutionsOrder.add(job);
				nextExecutionsOrder.sort(Comparator.comparing(
					Job::nextExecutionTimeInMillis
				));

				if(nextExecutionsOrder.size() == 1) {
					if(!shuttingDown) {
						threadPoolExecutor.execute(this::launcher);
					}
				} else {
					synchronized (launcherNotifier) {
						launcherNotifier.notify();
					}
				}
			}
		} else {
			job.status(JobStatus.DONE);
		}
	}

	@SneakyThrows
	private void launcher() {
		while(!shuttingDown) {
			long timeBeforeNextExecution = nextExecutionsOrder.get(0).nextExecutionTimeInMillis()
				- timeProvider.currentTime();

			if(timeBeforeNextExecution > 0L) {
				synchronized (launcherNotifier) {
					if(shuttingDown) {
						return;
					}
					launcherNotifier.wait(timeBeforeNextExecution);
				}
			} else {
				synchronized (this) {
					if(shuttingDown) {
						return;
					}

					Job jobToRun = nextExecutionsOrder.remove(0);
					threadPoolExecutor.execute(() -> runJob(jobToRun));
					// TODO check if the launcher does not need to remain alive like 10 min
					// if nextExecutionsOrder is empty <= for the use case where only one job is running very frequently
					if(nextExecutionsOrder.isEmpty()) {
						return;
					}
				}
			}
		}
	}

	private void runJob(Job jobToRun) {
		long startExecutionTime = timeProvider.currentTime();
		long timeBeforeNextExecution = jobToRun.nextExecutionTimeInMillis() - startExecutionTime;
		if(timeBeforeNextExecution < 0) {
			logger.debug("Job '{}' execution is {}ms late", jobToRun.name(), -timeBeforeNextExecution);
		}

		try {
			jobToRun.runnable().run();
		} catch(Throwable t) {
			logger.error("Error during job '{}' execution", jobToRun.name(), t);
		}
		jobToRun.executionsCount(jobToRun.executionsCount() + 1);
		jobToRun.lastExecutionTimeInMillis(timeProvider.currentTime());

		if(logger.isDebugEnabled()) {
			logger.debug(
				"Job '{}' executed in {}ms", jobToRun.name(),
				timeProvider.currentTime() - startExecutionTime
			);
		}

		if(shuttingDown) {
			return;
		}
		scheduleNextExecution(jobToRun);
	}

}

