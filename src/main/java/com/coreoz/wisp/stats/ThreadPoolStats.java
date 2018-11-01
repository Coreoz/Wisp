package com.coreoz.wisp.stats;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class ThreadPoolStats {

	private final int activeThreads;
	private final int idleThreads;

}
