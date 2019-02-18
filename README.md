Wisp Scheduler
==============

[![Build Status](https://travis-ci.org/Coreoz/Wisp.svg?branch=master)](https://travis-ci.org/Coreoz/Wisp)
[![Coverage Status](https://coveralls.io/repos/github/Coreoz/Wisp/badge.svg?branch=master)](https://coveralls.io/github/Coreoz/Wisp?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.coreoz/wisp/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.coreoz/wisp)

Wisp is a simple Java Scheduler with a minimal footprint.
Wisp weighs only 30Kb and has zero dependency except SLF4J.
It will only create threads that will be used: if two threads are enough to run all the jobs,
then only two threads will be created.
A third thread will only be created when 2 jobs have to run at the same time.

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
    <version>2.0.0</version>
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

Upgrade from version 1.x.x to version 2.x.x
-------------------------------------------
- If a cron schedule is used, then cron-utils must be upgraded to version 8.0.0
- The [monitor for long running jobs detection](#long-running-jobs-detection) might be configured

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
This feature is made possible by the use of [cron-utils](https://github.com/jmrozanec/cron-utils).
So to use cron expression, cron-utils should be added in the project:
```xml
<dependency>
    <groupId>com.cronutils</groupId>
    <artifactId>cron-utils</artifactId>
    <version>8.0.0</version>
</dependency>
```
Then to create a job which is executed every hour at the 30th minute,
you can create the schedule: `CronSchedule.parseQuartzCron("0 30 * * * ? *")`.

Cron expression should be created and checked using a tool like [Cron Maker](http://www.cronmaker.com/).

### Custom schedules
Custom schedules can be created,
see the [Schedule](src/main/java/com/coreoz/wisp/schedule/Schedule.java) interface.

### Past schedule
Schedules can reference a past time.
However once a past time is returned by a schedule,
the associated job will never be executed again.
At the first execution, if a past time is referenced a warning will be logged
but no exception will be raised.

Long running jobs detection
---------------------------

To detect jobs that are running for too long, an optional job monitor is provided.
It can be setup with:
```java
scheduler.schedule(
    new LongRunningJobMonitor(scheduler),
    Schedules.fixedDelaySchedule(Duration.ofMinutes(1))
);
```
This way, every minute, the monitor will check for jobs that are running for more than 5 minutes.
A warning message with the job stack trace will be logged for any job running for more than 5 minutes.

The detection threshold can also be configured this way: `new LongRunningJobMonitor(scheduler, Duration.ofMinutes(15))`

Scalable thread pool
--------------------

By default the thread pool size will only grow up from 0 to 10 threads (+ 1 thread reserved for the scheduler).
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

