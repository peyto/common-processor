package org.peyto.common.processor.core.schedule;

import org.peyto.common.processor.ProcessorTimeProvider;
import org.peyto.common.processor.core.ThreadSleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaemonSchedulerThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(DaemonSchedulerThread.class);

    private final DefaultProcessorScheduler scheduler;
    private final ProcessorTimeProvider timeProvider;
    private final ThreadSleeper threadSleeper;
    private final boolean isLogTimeline;

    public DaemonSchedulerThread(DefaultProcessorScheduler scheduler, ProcessorTimeProvider timeProvider, ThreadSleeper threadSleeper, boolean isLogTimeline) {
        super("processor-scheduler");
        super.setDaemon(true);
        this.scheduler = scheduler;
        this.timeProvider = timeProvider;
        this.threadSleeper = threadSleeper;
        this.isLogTimeline = isLogTimeline;
    }


    public void pingToRecalculate() {
        // just wake, the main thread will calc everything it needs
        // We need sync, because if we had 2 pings, it is possible to have race condition:
        // 1. after first ping nextWakeTime was calculated
        // 2. second ping received, but daemon thread hasn't gone to sleep yet. Ping will be ignored
        // 3. daemon thread goes to sleep, and the second update is ignored
        synchronized (threadSleeper) {
            threadSleeper.doNotify();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                long currentTimeMillis = timeProvider.getMillis();
                long nextWakeupMillis = scheduler.nextWakeupTimeMillis();
                if (nextWakeupMillis != DefaultProcessorScheduler.UNSET_TIMESTAMP && currentTimeMillis >= nextWakeupMillis) {
                    log.debug("notifying processor threads");
                    scheduler.notifyThreads(currentTimeMillis);
                } else {
                    synchronized (threadSleeper) {
                        nextWakeupMillis = scheduler.nextWakeupTimeMillis();
                        if (nextWakeupMillis == DefaultProcessorScheduler.UNSET_TIMESTAMP) {
                            log.debug("sleeping until notified");
                            threadSleeper.doWait();
                        } else {
                            long sleepMillis = nextWakeupMillis - currentTimeMillis;
                            if (sleepMillis > 0) {
                                if (isLogTimeline && log.isDebugEnabled()) {
                                    log.debug("Processors Scheduler Timeline: {}", scheduler.timelineAsLimitedString(currentTimeMillis));
                                }
                                log.debug("sleeping {} millis", sleepMillis);
                                threadSleeper.doWaitWithTimeout(sleepMillis);
                            } else {
                                log.debug("nextWakeup is scheduled in the past, not sleeping now to notify threads");
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            // We don't expect interruptions, so if it happened, probably app is stopping
            log.error("Scheduler Daemon Thread was interrupted at {}. Stopping", timeProvider.getMillis());
        }
    }
}
