package org.peyto.common.processor.core.schedule;

import org.peyto.common.processor.ProcessorThread;
import org.peyto.common.processor.ProcessorThreadListener;
import org.peyto.common.processor.ProcessorTimeProvider;
import org.peyto.common.processor.core.ThreadSleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.peyto.common.processor.utils.CheckerUtils.checkArg;

@Service
/**
 * The purpose of this service is to schedule, when processor threads should awake and ping them at specific timestamps
 */
public class DefaultProcessorScheduler implements ProcessorScheduler, ProcessorThreadListener {

    static final long UNSET_TIMESTAMP = -1L;
    private static final Logger log = LoggerFactory.getLogger(DefaultProcessorScheduler.class);

    private final DaemonSchedulerThread daemonSchedulerThread;

    private final Map<Long, ProcessorThread> threadIds = new ConcurrentHashMap<>();
    private final SortedMap<Long, LinkedHashSet<Long>> timeline = new TreeMap<>();

    // Inject thread sleeper for tests, so we can mock timings
    public DefaultProcessorScheduler(ProcessorTimeProvider processorTimeProvider, ThreadSleeper sleeper, @Value("#{new Boolean('${processor.scheduler.log.timeline:true}')}") boolean isLogTimeline) {
        this.daemonSchedulerThread = new DaemonSchedulerThread(this, processorTimeProvider, sleeper, isLogTimeline);
        daemonSchedulerThread.start();
    }

    @Override
    public void registerThread(long uniqueThreadId, ProcessorThread thread) {
        checkArg(!threadIds.containsKey(uniqueThreadId), "We can't have multiple threads with the same id! id = {}", uniqueThreadId);
        threadIds.put(uniqueThreadId, thread);
    }

    @Override
    public void schedule(long threadId, long timeMillis) {
        synchronized (timeline) {
            LinkedHashSet<Long> threadIdsToNotify = timeline.computeIfAbsent(timeMillis, aLong -> new LinkedHashSet<>());
            threadIdsToNotify.add(threadId);
        }
        log.debug("scheduling thread {} at {}", threadId, timeMillis);
        daemonSchedulerThread.pingToRecalculate();
    }

    @Override
    @Deprecated
    public void cancelAllScheduled(long threadId) {
        synchronized (timeline) {
            Iterator<Map.Entry<Long, LinkedHashSet<Long>>> iterator = timeline.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, LinkedHashSet<Long>> entry = iterator.next();
                LinkedHashSet<Long> threadIdsToNotify = entry.getValue();
                if (threadIdsToNotify.contains(threadId)) {
                    threadIdsToNotify.remove(threadId);
                    if (threadIdsToNotify.isEmpty()) {
                        iterator.remove();
                    }
                }
            }
        }
        // No need to ping daemon thread, we've just removed from the timeline, so it won't notify threads and there is no problem that daemon thread will wake additionally
    }

    @Override
    public void onFinish(long threadId) {
        threadIds.remove(threadId);
        // I don't want to cancel all scheduled wakeups => we'll just ignore them
    }

    long nextWakeupTimeMillis() {
        synchronized (timeline) {
            return !timeline.isEmpty() ? timeline.firstKey() : UNSET_TIMESTAMP;
        }
    }

    void notifyThreads(long currentMillis) {
        // we need to make sure ALL past timestamps are notified
        LinkedHashSet<Long> threadsToNotify = new LinkedHashSet<>();
        synchronized (timeline) {
            while (true) {
                if (!timeline.isEmpty()) {
                    Long firstTimestampInQueue = timeline.firstKey();
                    if (currentMillis >= firstTimestampInQueue) {
                        threadsToNotify.addAll(timeline.remove(firstTimestampInQueue));
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        // We move notify processors out of the sync block
        // This is executed from single thread only, no need for any additional synchronization
        for (Long threadId : threadsToNotify) {
            ProcessorThread thread = threadIds.get(threadId);
            if (thread != null) {
                log.debug("notifying processor thread id {}", threadId);
                thread.wakeProcessor();
            }
        }
    }

    String timelineAsLimitedString(long currentTimestampMillis) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        synchronized (timeline) {
            for (Long timestampMillis : timeline.keySet()) {
                if (builder.length() > 80) {
                    builder.setLength(builder.length() - 2); // remove last comma
                    builder.append("...]");
                    return builder.toString();
                }
                long delta = timestampMillis - currentTimestampMillis;
                if (timestampMillis == Long.MAX_VALUE) {
                    builder.append("MAX");
                } else if (delta > 600 * 1000) { // > 10 mins
                    builder.append(delta / 60000).append("m");
                } else if (delta > 10000) { // > 10 sec
                    builder.append(delta / 1000).append("s");
                } else { // < 10 sec
                    builder.append(delta).append("ms");
                    ;
                }

                builder.append(" : ");
                for (Long threadId : timeline.get(timestampMillis)) {
                    builder.append(threadId).append(",");
                }
                builder.setLength(builder.length() - 1); // remove last comma
                builder.append(", ");
            }
        }
        builder.setLength(builder.length() - 2); // remove last comma
        builder.append("]");
        return builder.toString();
    }
}
