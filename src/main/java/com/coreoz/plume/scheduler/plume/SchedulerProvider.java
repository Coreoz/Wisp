package com.coreoz.plume.scheduler.plume;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.coreoz.plume.scheduler.Scheduler;
import com.coreoz.plume.services.time.TimeProvider;

@Singleton
public class SchedulerProvider implements Provider<Scheduler> {

	private final Scheduler scheduler;

	@Inject
	public SchedulerProvider(TimeProvider timeProvider) {
		this.scheduler = new Scheduler(
			Scheduler.DEFAULT_THREAD_POOL_SIZE,
			Scheduler.DEFAULT_MINIMUM_DELAY_TO_REPLACE_JOB,
			new PlumeTimeProvider(timeProvider)
		);
	}

	@Override
	public Scheduler get() {
		return scheduler;
	}

}
