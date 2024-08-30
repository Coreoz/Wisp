package com.coreoz.wisp;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The default {@link ThreadFactory} for Wisp {@link Scheduler}
 */
class WispThreadFactory implements ThreadFactory {
    private static final AtomicInteger threadCounter = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, "Wisp Scheduler Worker #" + threadCounter.getAndIncrement());
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }
        return thread;
    }
}
