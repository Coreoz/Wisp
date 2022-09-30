package com.coreoz.wisp.time;

public class SystemTimeProvider implements TimeProvider {

	@Override
	public long currentTime() {
		return System.currentTimeMillis();
	}

}
