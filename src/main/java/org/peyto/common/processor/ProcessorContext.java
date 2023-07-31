package org.peyto.common.processor;

public interface ProcessorContext {

    void scheduleWakeup(long timeMillis);

    /**
     * Cancel all awakenings of the Processor Thread, scheduled earlier.
     *
     * @deprecated It's not recommended using this method, because additional scheduling shouldn't affect processor
     * state, processor should be idempotent. But cancelling can be pretty costly operation, since it needs to iterate
     * the whole timeline map of all processors. So, the method will be synchronized for some time, and it can affect
     * performance of other processor threads.
     */
    @Deprecated
    void cancelAllScheduledWakeups();

    long getCycleTimeMillis();

    long processorEndTimeMillis();


}
