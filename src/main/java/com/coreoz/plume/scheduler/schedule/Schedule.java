package com.coreoz.plume.scheduler.schedule;

public interface Schedule {

	static final long WILL_NOT_BE_EXECUTED_AGAIN = -1L;

	long nextExecutionInMillis(int executionsCount, long currentTimeInMillis);

}
