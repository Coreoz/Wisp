package com.coreoz.plume.scheduler.schedule;

import com.coreoz.plume.scheduler.time.TimeProvider;

public interface Schedule {

	long nextExecutionInMillis(TimeProvider timeProvider);

}
