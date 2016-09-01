package com.coreoz.plume.scheduler.guice;

import com.coreoz.plume.scheduler.Scheduler;
import com.coreoz.plume.scheduler.plume.SchedulerProvider;
import com.google.inject.AbstractModule;

public class GuiceSchedulerModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(Scheduler.class).toProvider(SchedulerProvider.class);
	}

}
