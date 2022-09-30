package com.coreoz.wisp.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class SchedulerStats {

	private final ThreadPoolStats threadPoolStats;

}
