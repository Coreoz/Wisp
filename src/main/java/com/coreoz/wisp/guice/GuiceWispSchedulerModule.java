package com.coreoz.wisp.guice;

import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.plume.SchedulerProvider;
import com.google.inject.AbstractModule;

public class GuiceWispSchedulerModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(Scheduler.class).toProvider(SchedulerProvider.class);
	}

}
