Wisp Scheduler
==============

[![Build Status](https://travis-ci.org/Coreoz/Wisp.svg?branch=master)](https://travis-ci.org/Coreoz/Wisp)
[![Coverage Status](https://coveralls.io/repos/github/Coreoz/Wisp/badge.svg?branch=master)](https://coveralls.io/github/Coreoz/Wisp?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.coreoz/wisp/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.coreoz/wisp)

Wisp is a library for managing the execution of recurring Java jobs.
It works like the Java class `ScheduledThreadPoolExecutor`, but it comes with some advanced features:
- [Jobs can be scheduled to run](#schedules) according to: a fixed hour (e.g. 00:30), a CRON expression, or a custom code-based expression,
- [Statistics](#statistics) about each job execution can be retrieved,
- A [too long jobs detection mechanism](#long-running-jobs-detection) can be configured,
- The [thread pool can be configured to scale down](#scalable-thread-pool) when there is less jobs to execute concurrently.

Wisp weighs only 30Kb and has zero dependency except SLF4J for logging.
It will try to only create threads that will be used: if one thread is enough to run all the jobs,
then only one thread will be created.
A second thread will generally be created only when 2 jobs have to run at the same time.

The scheduler precision will depend on the system load.
Though a job will never be executed early, it will generally run after 1ms of the scheduled time.

Wisp is compatible with Java 8 and higher.

Getting started
---------------

Include Wisp in your project:
```xml
<dependency>
    <groupId>com.coreoz</groupId>
    <artifactId>wisp</artifactId>
    <version>2.3.0</version>
</dependency>
```

Schedule a job:
```java
Scheduler scheduler = new Scheduler();

scheduler.schedule(
    () -> System.out.println("My first job"),           // the runnable to be scheduled
    Schedules.fixedDelaySchedule(Duration.ofMinutes(5)) // the schedule associated to the runnable
);
```
Done!

A project should generally contain only one instance of a `Scheduler`.
So either a dependency injection framework handles this instance,
or either a static instance of `Scheduler` should be created.

In production, it is generally a good practice to configure the
[monitor for long running jobs detection](#long-running-jobs-detection).

Changelog and upgrade instructions
----------------------------------
All the changelog and the upgrades instructions are available
in the [project releases page](https://github.com/Coreoz/Wisp/releases).

Schedules
---------

When a job is created or done executing, the schedule associated to the job
is called to determine when the job should next be executed.
There are multiple implications:
- the same job will never be executed twice at a time,
- if a job has to be executed at a fixed frequency,
then the job has to finish running before the next execution is scheduled ;
else the next execution will likely be skipped (depending of the `Schedule` implementation). 

### Basics schedules
Basics schedules are referenced in the `Schedules` class:
- `fixedDelaySchedule(Duration)`: execute a job at a fixed delay after each execution
- `executeAt(String)`: execute a job at the same time every day, e.g. `executeAt("05:30")`

### Composition
Schedules are very flexible and can easily be composed, e.g:
- `Schedules.afterInitialDelay(Schedules.fixedDelaySchedule(Duration.ofMinutes(5)), Duration.ZERO)`:
the job will be first executed ASAP and then with a fixed delay of 5 minutes between each execution,
- `Schedules.executeOnce(Schedules.executeAt("05:30"))`: the job will be executed once at 05:30.
- `Schedules.executeOnce(Schedules.fixedDelaySchedule(Duration.ofSeconds(10)))`:
the job will be executed once 10 seconds after it has been scheduled.

### Cron
Schedules can be created using [cron expressions](https://en.wikipedia.org/wiki/Cron#CRON_expression).
This feature is made possible by the use of [cron library](https://github.com/frode-carlsen/cron). This library is very lightweight: it has no dependency and is made of a single Java class of 650 lines of code.

So to use cron expression, this library has to be added:
```xml
<dependency>
    <groupId>ch.eitchnet</groupId>
    <artifactId>cron</artifactId>
    <version>1.6.2</version>
</dependency>
```

Then to create a job which is executed every hour at the 30th minute,
you can create the [schedule](#schedules) using: `CronExpressionSchedule.parse("30 * * * *")`.

`CronExpressionSchedule` exposes two methods to create Cron expressions:
- `CronExpressionSchedule.parse()` to parse a 5 fields Cron expression (Unix standard), so without a second field
- `CronExpressionSchedule.parseWithSeconds()` to parse a 6 fields Cron expression, so the first field is the second


Cron expression should be checked using a tool like:
- [Cronhub](https://crontab.cronhub.io/)
- [Freeformater](https://www.freeformatter.com/cron-expression-generator-quartz.html) *but be careful to not include the year field. So for the Cron expression `25 * * * * * *` (to run every minute at the second 25), the correct expression must be `25 * * * * *`*

Sometimes a use case is to disable a job through configuration. This use case can be addressed by setting a Cron expression that looks up the 31st of February: `* * * 31 2 *`

Cron-utils was the default Cron implementation before Wisp 2.2.2. This has [changed in version 2.3.0](/../../issues/14).
Documentation about cron-utils implementation can be found at [Wisp 2.2.2](/../../tree/2.2.2#cron).
Migration from cron-utils is detailed in the [release note of Wisp 2.3.0](/../../releases/tag/2.3.0)

### Custom schedules
Custom schedules can be created,
see the [Schedule](src/main/java/com/coreoz/wisp/schedule/Schedule.java) interface.

### Past schedule
Schedules can reference a past time.
However once a past time is returned by a schedule,
the associated job will never be executed again.
At the first execution, if a past time is referenced a warning will be logged
but no exception will be raised.

Statistics
----------
Two methods enable to fetch scheduler statistics:
- `Scheduler.jobStatus()`: To fetch all the jobs executing on the scheduler. For each job, these data are available:
  - name,
  - status (see `JobStatus` for details),
  - executions count,
  - last execution start date,
  - last execution end date,
  - next execution date.
- `Scheduler.stats()`: To fetch statistics about the underlying thread pool:
  - min threads,
  - max threads,
  - active threads running jobs,
  - idle threads,
  - largest thread pool size.

Long running jobs detection
---------------------------

To detect jobs that are running for too long, an optional job monitor is provided.
It can be setup with:
```java
scheduler.schedule(
    "Long running job monitor",
    new LongRunningJobMonitor(scheduler),
    Schedules.fixedDelaySchedule(Duration.ofMinutes(1))
);
```
This way, every minute, the monitor will check for jobs that are running for more than 5 minutes.
A warning message with the job stack trace will be logged for any job running for more than 5 minutes.

The detection threshold can also be configured this way: `new LongRunningJobMonitor(scheduler, Duration.ofMinutes(15))`

Scalable thread pool
--------------------

By default the thread pool size will only grow up, from 0 to 10 threads (and not scale down).
But it is also possible to define a maximum keep alive duration after which idle threads will be removed from the pool.
This can be configured this way:
```java
Scheduler scheduler = new Scheduler(
    SchedulerConfig
        .builder()
        .minThreads(2)
        .maxThreads(15)
        .threadsKeepAliveTime(Duration.ofHours(1))
        .build()
);
```
In this example:
- There will be always at least 2 threads to run the jobs,
- The thread pool can grow up to 15 threads to run the jobs,
- Idle threads for at least an hour will be removed from the pool, until the 2 minimum threads remain.

Plume Framework integration
---------------------------

If you are already using [Plume Framework](https://github.com/Coreoz/Plume),
please take a look at [Plume Scheduler](https://github.com/Coreoz/Plume/tree/master/plume-scheduler).

