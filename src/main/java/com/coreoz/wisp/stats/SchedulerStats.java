package com.coreoz.wisp.stats;

import lombok.Value;

@Value(staticConstructor = "of")
public class SchedulerStats {

	private final ThreadPoolStats threadPoolStats;

}
