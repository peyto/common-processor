package org.peyto.common.processor.core;

/**
 * Basically, extract wait/notify objects, so we can override them in tests for control
 */
public interface ThreadSleeper {

    void doNotify();

    void doWait() throws InterruptedException;

    void doWaitWithTimeout(long sleepMillis) throws InterruptedException;
}
