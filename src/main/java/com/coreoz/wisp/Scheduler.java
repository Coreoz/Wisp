package com.coreoz.wisp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

	private static final AtomicInteger threadCounter = new AtomicInteger(0);

	/**
	 * @deprecated Default values are available in {@link SchedulerConfig}
	 * It will be deleted in version 3.0.0.
	 */
	@Deprecated
	public static final int DEFAULT_THREAD_POOL_SIZE = 10;
	/**
	 * @deprecated This value is not used anymore
	 * It will be deleted in version 3.0.0.
	 */
	@Deprecated
	public static final long DEFAULT_MINIMUM_DELAY_IN_MILLIS_TO_REPLACE_JOB = 10L;

	private final ThreadPoolExecutor threadPoolExecutor;
	private final TimeProvider timeProvider;
	private final AtomicBoolean launcherNotifier;

	// jobs
	private final Map<String, Job> indexedJobsByName;
	private final ArrayList<Job> nextExecutionsOrder;
	private final Map<String, CompletableFuture<Job>> cancelHandles;

	private volatile boolean shuttingDown;

	// constructors

	/**
	 * Create a scheduler with the defaults defined at {@link SchedulerConfig}
	 */
	public Scheduler() {
		this(SchedulerConfig.builder().build());
	}

	/**
	 * Create a scheduler with the defaults defined at {@link SchedulerConfig}
	 * and with a max number of worker threads
	 * @param maxThreads The maximum number of worker threads that can be created for the scheduler.
	 * @throws IllegalArgumentException if {@code maxThreads <= 0}
	 */
	public Scheduler(int maxThreads) {
		this(SchedulerConfig.builder().maxThreads(maxThreads).build());
	}

	/**
	 * Create a scheduler according to the configuration
	 * @throws IllegalArgumentException if one of the following holds:<br>
     * {@code SchedulerConfig#getMinThreads() < 0}<br>
     * {@code SchedulerConfig#getThreadsKeepAliveTime() < 0}<br>
     * {@code SchedulerConfig#getMaxThreads() <= 0}<br>
     * {@code SchedulerConfig#getMaxThreads() < SchedulerConfig#getMinThreads()}
     * @throws NullPointerException if {@code SchedulerConfig#getTimeProvider()} is {@code null}
	 */
	public Scheduler(SchedulerConfig config) {
		if(config.getTimeProvider() == null) {
			throw new NullPointerException("The timeProvider cannot be null");
		}

		this.indexedJobsByName = new ConcurrentHashMap<>();
		this.nextExecutionsOrder = new ArrayList<>();
		this.timeProvider = config.getTimeProvider();
		this.launcherNotifier = new AtomicBoolean(true);
		this.cancelHandles = new ConcurrentHashMap<>();
		this.threadPoolExecutor = new ScalingThreadPoolExecutor(
			config.getMinThreads(),
			config.getMaxThreads(),
			config.getThreadsKeepAliveTime().toMillis(),
			TimeUnit.MILLISECONDS,
			new WispThreadFactory()
		);
		// run job launcher thread
		Thread launcherThread = new Thread(this::launcher, "Wisp Monitor");
		if (launcherThread.isDaemon()) {
			launcherThread.setDaemon(false);
		}
		launcherThread.start();
	}

	/**
	 * @deprecated Use {@link #Scheduler(SchedulerConfig)} to specify multiple configuration values.
	 * It will be deleted in version 3.0.0.
	 * @throws IllegalArgumentException if {@code maxThreads <= 0}
	 */
	@Deprecated
	public Scheduler(int maxThreads, long minimumDelayInMillisToReplaceJob) {
		this(maxThreads, minimumDelayInMillisToReplaceJob, new SystemTimeProvider());
	}

	/**
	 * @deprecated Use {@link #Scheduler(SchedulerConfig)} to specify multiple configuration values.
	 * It will be deleted in version 3.0.0.
	 * @throws IllegalArgumentException if {@code maxThreads <= 0}
	 * @throws NullPointerException if {@code timeProvider} is {@code null}
	 */
	@Deprecated
	public Scheduler(int maxThreads, long minimumDelayInMillisToReplaceJob,
			TimeProvider timeProvider) {
		this(SchedulerConfig
			.builder()
			.maxThreads(maxThreads)
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
	 * @throws NullPointerException if {@code runnable} or {@code when} are {@code null}
	 * @throws IllegalArgumentException if the same instance of {@code runnable} is
	 * scheduled twice whereas the corresponding job status is not {@link JobStatus#DONE}
	 */
	public Job schedule(Runnable runnable, Schedule when) {
		return schedule(null, runnable, when);
	}

	/**
	 * Schedule the executions of a process.<br>
	 * <br>
	 * If a job already exists with the same name and has the status {@link JobStatus#DONE},
	 * then the created job will inherit the stats of the existing done job:
	 * {@link Job#executionsCount()} and {@link Job#lastExecutionTimeInMillis()}
	 *
	 * @param nullableName The name of the created job
	 * @param runnable The process to be executed at a schedule
	 * @param when The {@link Schedule} at which the process will be executed
	 * @return The corresponding {@link Job} created.
	 * @throws NullPointerException if {@code runnable} or {@code when} are {@code null}
	 * @throws IllegalArgumentException if the same {@code nullableName} is
	 * scheduled twice whereas the corresponding job status is not {@link JobStatus#DONE}
	 */
	public Job schedule(String nullableName, Runnable runnable, Schedule when) {
		Objects.requireNonNull(runnable, "Runnable must not be null");
		Objects.requireNonNull(when, "Schedule must not be null");

		String name = nullableName == null ? runnable.toString() : nullableName;

		Job job = prepareJob(name, runnable, when);
		long currentTimeInMillis = timeProvider.currentTime();
		if(when.nextExecutionInMillis(
			currentTimeInMillis,
			job.executionsCount(),
			job.lastExecutionEndedTimeInMillis()
			) < currentTimeInMillis) {
			logger.warn("The job '{}' is scheduled at a paste date: it will never be executed", name);
		}

		logger.info("Scheduling job '{}' to run {}", job.name(), job.schedule());
		scheduleNextExecution(job);

		return job;
	}

	/**
	 * Fetch the status of all the jobs that has been registered on the {@code Scheduler}
	 * including the {@link JobStatus#DONE} jobs
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
		Job job = findJob(jobName).orElseThrow(IllegalArgumentException::new);

		synchronized (this) {
			JobStatus jobStatus = job.status();
			if(jobStatus == JobStatus.DONE) {
				return CompletableFuture.completedFuture(job);
			}
			CompletableFuture<Job> existingHandle = cancelHandles.get(jobName);
			if(existingHandle != null) {
				return existingHandle;
			}

			job.schedule(Schedule.willNeverBeExecuted);
			if(jobStatus == JobStatus.READY && threadPoolExecutor.remove(job.runningJob())) {
				scheduleNextExecution(job);
				return CompletableFuture.completedFuture(job);
			}

			if(jobStatus == JobStatus.RUNNING
				// if the job status is/was READY but could not be removed from the thread pool,
				// then we have to wait for it to finish
				|| jobStatus == JobStatus.READY) {
				CompletableFuture<Job> promise = new CompletableFuture<>();
				cancelHandles.put(jobName, promise);
				return promise;
			} else {
				for (Iterator<Job> iterator = nextExecutionsOrder.iterator(); iterator.hasNext();) {
					Job nextJob = iterator.next();
					if(nextJob == job) {
						iterator.remove();
						job.status(JobStatus.DONE);
						return CompletableFuture.completedFuture(job);
					}
				}
				throw new IllegalStateException(
					"Cannot find the job " + job + " in " + nextExecutionsOrder
					+ ". Please open an issue on https://github.com/Coreoz/Wisp/issues"
				);
			}
		}
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
		logger.info("Shutting down...");

		if(!shuttingDown) {
			synchronized (this) {
				shuttingDown = true;
				threadPoolExecutor.shutdown();
			}

			// stops jobs that have not yet started to be executed
			for(Job job : jobStatus()) {
				Runnable runningJob = job.runningJob();
				if(runningJob != null) {
					threadPoolExecutor.remove(runningJob);
				}
				job.status(JobStatus.DONE);
			}
			synchronized (launcherNotifier) {
				launcherNotifier.set(false);
				launcherNotifier.notify();
			}
		}

		threadPoolExecutor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
	}

	/**
	 * Fetch statistics about the current {@code Scheduler}
	 */
	public SchedulerStats stats() {
		int activeThreads = threadPoolExecutor.getActiveCount();
		return SchedulerStats.of(ThreadPoolStats.of(
			threadPoolExecutor.getCorePoolSize(),
			threadPoolExecutor.getMaximumPoolSize(),
			activeThreads,
			threadPoolExecutor.getPoolSize() - activeThreads,
			threadPoolExecutor.getLargestPoolSize()
		));
	}

	// internal

	private Job prepareJob(String name, Runnable runnable, Schedule when) {
		// lock needed to make sure 2 jobs with the same name are not submitted at the same time
		synchronized (indexedJobsByName) {
			Job lastJob = findJob(name).orElse(null);

			if(lastJob != null && lastJob.status() != JobStatus.DONE) {
				throw new IllegalArgumentException("A job is already scheduled with the name:" + name);
			}

			Job job = new Job(
				JobStatus.SCHEDULED,
				0L,
				lastJob != null ? lastJob.executionsCount() : 0,
				lastJob != null ? lastJob.lastExecutionStartedTimeInMillis() : null,
				lastJob != null ? lastJob.lastExecutionEndedTimeInMillis() : null,
				name,
				when,
				runnable
			);
			indexedJobsByName.put(name, job);

			return job;
		}
	}

	private synchronized void scheduleNextExecution(Job job) {
		// clean up
		job.runningJob(null);

		// next execution time calculation
		long currentTimeInMillis = timeProvider.currentTime();
		try {
			job.nextExecutionTimeInMillis(
				job.schedule().nextExecutionInMillis(
					currentTimeInMillis, job.executionsCount(), job.lastExecutionEndedTimeInMillis()
				)
			);
		} catch (Throwable t) {
			logger.error(
				"An exception was raised during the job next execution time calculation,"
				+ " therefore the job '{}' will not be executed again.",
				job.name(),
				t
			);
			job.nextExecutionTimeInMillis(Schedule.WILL_NOT_BE_EXECUTED_AGAIN);
		}

		// next execution planning
		if(job.nextExecutionTimeInMillis() >= currentTimeInMillis) {
			job.status(JobStatus.SCHEDULED);
			nextExecutionsOrder.add(job);
			nextExecutionsOrder.sort(Comparator.comparing(
				Job::nextExecutionTimeInMillis
			));

			synchronized (launcherNotifier) {
				launcherNotifier.set(false);
				launcherNotifier.notify();
			}
		} else {
			logger.info(
				"Job '{}' will not be executed again since its next execution time, {}ms, is planned in the past",
				job.name(),
				Instant.ofEpochMilli(job.nextExecutionTimeInMillis())
			);
			job.status(JobStatus.DONE);

			CompletableFuture<Job> cancelHandle = cancelHandles.remove(job.name());
			if(cancelHandle != null) {
				cancelHandle.complete(job);
			}
		}
	}

	/**
	 * The daemon that will be in charge of placing the jobs in the thread pool
	 * when they are ready to be executed.
	 */
	@SneakyThrows
	private void launcher() {
		while(!shuttingDown) {
			Long timeBeforeNextExecution = null;
			synchronized (this) {
				if(nextExecutionsOrder.size() > 0) {
					timeBeforeNextExecution = nextExecutionsOrder.get(0).nextExecutionTimeInMillis()
						- timeProvider.currentTime();
				}
			}

			if(timeBeforeNextExecution == null || timeBeforeNextExecution > 0L) {
				synchronized (launcherNotifier) {
					if(shuttingDown) {
						return;
					}
					// If someone has notified the launcher
					// then the launcher must check again the next job to execute.
					// We must be sure not to miss any changes that would have
					// happened after the timeBeforeNextExecution calculation.
					if(launcherNotifier.get()) {
						if(timeBeforeNextExecution == null) {
							launcherNotifier.wait();
						} else {
							launcherNotifier.wait(timeBeforeNextExecution);
						}
					}
					launcherNotifier.set(true);
				}
			} else {
				synchronized (this) {
					if(shuttingDown) {
						return;
					}

					if(nextExecutionsOrder.size() > 0) {
						Job jobToRun = nextExecutionsOrder.remove(0);
						jobToRun.status(JobStatus.READY);
						jobToRun.runningJob(() -> runJob(jobToRun));
						if(threadPoolExecutor.getActiveCount() == threadPoolExecutor.getMaximumPoolSize()) {
							logger.warn(
								"Job thread pool is full, either tasks take too much time to execute"
								+ " or either the thread pool is too small"
							);
						}
						threadPoolExecutor.execute(jobToRun.runningJob());
					}
				}
			}
		}
	}

	/**
	 * The wrapper around a job that will be executed in the thread pool.
	 * It is especially in charge of logging, changing the job status
	 * and checking for the next job to be executed.
	 * @param jobToRun the job to execute
	 */
	private void runJob(Job jobToRun) {
		long startExecutionTime = timeProvider.currentTime();
		long timeBeforeNextExecution = jobToRun.nextExecutionTimeInMillis() - startExecutionTime;
		if(timeBeforeNextExecution < 0) {
			logger.debug("Job '{}' execution is {}ms late", jobToRun.name(), -timeBeforeNextExecution);
		}
		jobToRun.status(JobStatus.RUNNING);
		jobToRun.lastExecutionStartedTimeInMillis(startExecutionTime);
		jobToRun.threadRunningJob(Thread.currentThread());

		try {
			jobToRun.runnable().run();
		} catch(Throwable t) {
			logger.error("Error during job '{}' execution", jobToRun.name(), t);
		}
		jobToRun.executionsCount(jobToRun.executionsCount() + 1);
		jobToRun.lastExecutionEndedTimeInMillis(timeProvider.currentTime());
		jobToRun.threadRunningJob(null);

		if(logger.isDebugEnabled()) {
			logger.debug(
				"Job '{}' executed in {}ms", jobToRun.name(),
				timeProvider.currentTime() - startExecutionTime
			);
		}

		if(shuttingDown) {
			return;
		}
		synchronized (this) {
			scheduleNextExecution(jobToRun);
		}
	}

	private static class WispThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "Wisp Scheduler Worker #" + threadCounter.getAndIncrement());
			if (thread.isDaemon()) {
				thread.setDaemon(false);
			}
			if (thread.getPriority() != Thread.NORM_PRIORITY) {
				thread.setPriority(Thread.NORM_PRIORITY);
			}
			return thread;
		}
	}

}
