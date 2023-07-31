package org.peyto.common.processor.core.schedule;

import org.peyto.common.processor.ProcessorThread;
import org.peyto.common.processor.ProcessorThreadListener;

public interface ProcessorScheduler extends ProcessorThreadListener {

    void registerThread(long threadId, ProcessorThread thread);

    void schedule(long threadId, long timeMillis);

    /**
     * Cancel all schedule wakes of the Processor Thread, scheduled earlier.
     *
     * @deprecated It's better not to use this method, because additional scheduling shouldn't affect processor state,
     * it should be idempotent. But cancelling can be pretty costly operation, since it needs to iterate the whole
     * timeline map. The timeline is based on the timestamps and there is no index to find scheduled threads.
     * So, the method will be synchronized for some time, and it can affect performance of other processor threads.
     *
     * @param threadId Processor thread id to cancel all scheduling
     */
    @Deprecated
    void cancelAllScheduled(long threadId);

}
