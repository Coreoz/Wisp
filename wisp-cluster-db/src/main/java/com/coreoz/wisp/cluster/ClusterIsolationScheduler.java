package com.coreoz.wisp.cluster;

import java.util.Objects;

import com.coreoz.wisp.Job;
import com.coreoz.wisp.Scheduler;
import com.coreoz.wisp.schedule.Schedule;

public class ClusterIsolationScheduler {

    private final Scheduler scheduler;

    public ClusterIsolationScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Job schedule(String name, Runnable runnable, String frequency) {
        Objects.requireNonNull(name, "Name must not be null");
        Objects.requireNonNull(runnable, "Runnable must not be null");
        Objects.requireNonNull(frequency, "Schedule must not be null");

        createJob(name, frequency);
        Schedule schedule = parseFrequency(frequency);
        return scheduler.schedule(
            name,
            () -> executeOnlyOnOneInstance(name, runnable, schedule),
            parseFrequency(frequency)
        );
    }

    private void createJob(String jobName, String frequency) {
        // select -> to avoid most of the time the create exception issue
        // try catch create (jobName, nextExecutionTime = 0)
    }
    
    private Schedule parseFrequency(String frequency) {
    	return null;
    }

    private void executeOnlyOnOneInstance(String name, Runnable runnable, Schedule schedule) {
        // select last execution time
        // next execution time = schedule.next(lastExecutionTime, 0, lastExecutionTime);
        // SI next execution time < now
        // update wisp_cluster set last_execution_time = expected_execution_time where job_name = name AND last_execution_time < expected_execution_time)
        // si UPDATE ok, then runnable !
    }

    // tests

    // - one job creation
    // - n threads executing n times the jobs
    // - verify that the job has been executed n times

}
