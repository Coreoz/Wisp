package com.coreoz.wisp.guice;

import com.coreoz.plume.guice.GuiceServicesModule;
import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.plume.SchedulerProvider;
import com.google.inject.AbstractModule;

public class GuiceWispSchedulerModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new GuiceServicesModule());
		bind(Scheduler.class).toProvider(SchedulerProvider.class);
	}

}
