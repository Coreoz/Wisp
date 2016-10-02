package com.coreoz.wisp.stats;

import lombok.Value;

@Value(staticConstructor = "of")
public class ThreadPoolStats {

	private final int activeThreads;
	private final int idleThreads;

}
