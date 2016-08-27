package com.coreoz.plume.scheduler.time;

public class SystemTimeProvider implements TimeProvider {

	@Override
	public long currentTime() {
		return System.currentTimeMillis();
	}

}
