package com.coreoz.wisp;

import java.time.Duration;

import com.coreoz.wisp.time.TimeProvider;

public class SchedulerConfiguration {

	private int minThreads;
	private int maxThreads;
	private Duration threadsKeepAliveTime;
	private TimeProvider timeProvider;

}
