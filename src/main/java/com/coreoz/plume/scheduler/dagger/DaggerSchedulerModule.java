package com.coreoz.plume.scheduler.dagger;

import javax.inject.Singleton;

import com.coreoz.plume.dagger.DaggerCoreModule;
import com.coreoz.plume.scheduler.Scheduler;
import com.coreoz.plume.scheduler.plume.SchedulerProvider;

import dagger.Module;
import dagger.Provides;

@Module(includes = DaggerCoreModule.class)
public class DaggerSchedulerModule {

	@Provides
	@Singleton
	static Scheduler provideScheduler(SchedulerProvider schedulerProvider) {
		return schedulerProvider.get();
	}

}
