package com.coreoz.wisp.schedule.cron;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import com.coreoz.wisp.schedule.Schedule;
import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

/**
 * A {@link Schedule} based on a <a href="https://en.wikipedia.org/wiki/Cron#CRON_expression">
 * cron expression</a>.<br/>
 * <br/>
 * This class depends on <a href="https://github.com/jmrozanec/cron-utils">cron-utils</a>,
 * so this dependency have to be in the classpath in order to be able to use {@link CronSchedule}.
 * Since cron-utils is marked as optional, it has to be explicitly referenced in the
 * project dependency configuration (pom.xml, build.gradle, build.sbt etc.).
 */
public class CronSchedule implements Schedule {

	private static final CronParser UNIX_CRON_PARSER = new CronParser(
		CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
	);
	private static final CronParser QUARTZ_CRON_PARSER = new CronParser(
		CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ)
	);

	private static final CronDescriptor ENGLISH_DESCRIPTOR = CronDescriptor.instance(Locale.ENGLISH);

	private final ExecutionTime cronExpression;
	private final String description;

	public CronSchedule(Cron cronExpression) {
		this.cronExpression = ExecutionTime.forCron(cronExpression);
		this.description = ENGLISH_DESCRIPTOR.describe(cronExpression);
	}

	@Override
	public long nextExecutionInMillis(int executionsCount, long currentTimeInMillis) {
		return cronExpression.nextExecution(ZonedDateTime.ofInstant(
			Instant.ofEpochMilli(currentTimeInMillis),
			ZoneId.systemDefault()
		))
		.toInstant()
		.toEpochMilli();
	}



	@Override
	public String toString() {
		return description;
	}

	/**
	 * Create a {@link Schedule} from a cron expression based on the Unix format,
	 * e.g. 1 * * * * for each minute.
	 */
	public static CronSchedule parseUnixCron(String cronExpression) {
		return new CronSchedule(UNIX_CRON_PARSER.parse(cronExpression));
	}

	/**
	 * Create a {@link Schedule} from a cron expression based on the Quartz format,
	 * e.g. 0 * * * * ? * for each minute.
	 */
	public static CronSchedule parseQuartzCron(String cronExpression) {
		return new CronSchedule(QUARTZ_CRON_PARSER.parse(cronExpression));
	}

}
