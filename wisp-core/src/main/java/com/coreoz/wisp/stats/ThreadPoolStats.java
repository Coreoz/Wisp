package com.coreoz.wisp.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class ThreadPoolStats {

	private final int minThreads;
	private final int maxThreads;
	private final int activeThreads;
	private final int idleThreads;
	private final int largestPoolSize;

}
