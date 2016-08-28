package com.coreoz.plume.scheduler.stats;

import lombok.Value;

@Value(staticConstructor = "of")
public class SchedulerStats {

	private final ThreadPoolStats threadPoolStats;

}
