package com.coreoz.wisp.schedule.cron;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.coreoz.wisp.schedule.Schedule;

import fc.cron.CronExpression;

/**
 * A {@link Schedule} based on a <a href="https://en.wikipedia.org/wiki/Cron#CRON_expression">
 * Cron expression</a>.<br>
 * <br>
 * This class depends on <a href="https://github.com/frode-carlsen/cron">Cron library</a>,
 * so this dependency has to be in the classpath in order to be able to use {@link CronExpressionSchedule}.
 * Since the Cron library is marked as optional in Wisp, it has to be
 * <a href="https://github.com/Coreoz/Wisp#cron">explicitly referenced in the project dependency configuration</a>
 * (pom.xml, build.gradle, build.sbt etc.).<br>
 * <br>
 * See also {@link CronExpression} for format details and implementation.
 */
public class CronExpressionSchedule implements Schedule {

	private final CronExpression cronExpression;
	private final ZoneId zoneId;

	public CronExpressionSchedule(CronExpression cronExpression, ZoneId zoneId) {
		this.cronExpression = cronExpression;
		this.zoneId = zoneId;
	}

	public CronExpressionSchedule(CronExpression cronExpression) {
		this(cronExpression, ZoneId.systemDefault());
	}

	@Override
	public long nextExecutionInMillis(long currentTimeInMillis, int executionsCount, Long lastExecutionTimeInMillis) {
		Instant currentInstant = Instant.ofEpochMilli(currentTimeInMillis);
		try {
			return cronExpression.nextTimeAfter(ZonedDateTime.ofInstant(
				currentInstant,
				zoneId
			)).toEpochSecond() * 1000L;
		} catch (IllegalArgumentException e) {
			return Schedule.WILL_NOT_BE_EXECUTED_AGAIN;
		}
	}

	@Override
	public String toString() {
		return cronExpression.toString();
	}

	/**
	 * Create a {@link Schedule} from a cron expression based on the Unix format,
	 * e.g. 1 * * * * for each minute.
	 */
	public static CronExpressionSchedule parse(String cronExpression) {
		return new CronExpressionSchedule(CronExpression.createWithoutSeconds(cronExpression));
	}

	/**
	 * Create a {@link Schedule} from a cron expression based on the Unix format, but accepting a second field as the first one,
	 * e.g. 29 * * * * * for each minute at the second 29, for instance 12:05:29.
	 */
	public static CronExpressionSchedule parseWithSeconds(String cronExpression) {
		return new CronExpressionSchedule(CronExpression.create(cronExpression));
	}

}
