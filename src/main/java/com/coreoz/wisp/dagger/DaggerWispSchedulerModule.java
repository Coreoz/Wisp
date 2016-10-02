package com.coreoz.wisp.dagger;

import javax.inject.Singleton;

import com.coreoz.plume.dagger.DaggerServicesModule;
import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.plume.SchedulerProvider;

import dagger.Module;
import dagger.Provides;

@Module(includes = DaggerServicesModule.class)
public class DaggerWispSchedulerModule {

	@Provides
	@Singleton
	static Scheduler provideScheduler(SchedulerProvider schedulerProvider) {
		return schedulerProvider.get();
	}

}
