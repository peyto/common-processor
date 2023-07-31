package org.peyto.common.processor.core;

import org.springframework.stereotype.Service;

@Service
public class DefaultThreadSleeper implements ThreadSleeper {

    @Override
    public synchronized void doWait() throws InterruptedException {
        this.wait();
    }

    @Override
    public synchronized void doWaitWithTimeout(long sleepMillis) throws InterruptedException {
        this.wait(sleepMillis);
    }

    @Override
    public synchronized void doNotify() {
        this.notify();
    }

}
