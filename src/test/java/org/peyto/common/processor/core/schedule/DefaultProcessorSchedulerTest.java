package org.peyto.common.processor.core.schedule;

import org.peyto.common.processor.ProcessorThread;
import org.peyto.common.processor.ProcessorTimeProvider;
import org.peyto.common.processor.core.ThreadSleeper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class DefaultProcessorSchedulerTest {

    private MockedTimeProvider timeProvider;
    private MockedThreadSleeper sleeper;

    private ProcessorScheduler defaultProcessorScheduler;

    @Before
    public void init() {
        timeProvider = new MockedTimeProvider(0);
        sleeper = new MockedThreadSleeper(timeProvider);
        defaultProcessorScheduler = new DefaultProcessorScheduler(timeProvider, sleeper, true);
    }

    @Test(timeout = 10000)
    public void simpleSchedule() throws Exception {
        ProcessorThread thread1 = mockProcessorThread(1);
        defaultProcessorScheduler.schedule(1, timeProvider.getMillis() + 5);
        blockTestUntilDaemonThreadSettled();
        assertSleepingWithTimeout(5);

        // 5 ms passed
        timeProvider.increase(5);
        sleeper.wakedByTimeout();

        blockTestUntilDaemonThreadSettled();
        assertSleepingWithoutTimeout(); // no more schedules - just sleeping
        Mockito.verify(thread1).wakeProcessor();
    }

    @Test(timeout = 10000)
    public void multipleSchedules() throws Exception {
        ProcessorThread thread1 = mockProcessorThread(1);
        ProcessorThread thread2 = mockProcessorThread(2);

        defaultProcessorScheduler.schedule(1, timeProvider.getMillis() + 5);
        defaultProcessorScheduler.schedule(2, timeProvider.getMillis() + 8);
        blockTestUntilDaemonThreadSettled();
        assertSleepingWithTimeout(5);

        // 5 ms passed
        timeProvider.increase(5);
        sleeper.wakedByTimeout();

        blockTestUntilDaemonThreadSettled();
        assertSleepingWithTimeout(3);
        Mockito.verify(thread1).wakeProcessor();

        // 3 more ms passed
        timeProvider.increase(3);
        sleeper.wakedByTimeout();

        blockTestUntilDaemonThreadSettled();
        assertSleepingWithoutTimeout();
        Mockito.verify(thread2).wakeProcessor();
    }

    @Test(timeout = 10000)
    public void scheduleBeforeExisting() throws Exception {
        ProcessorThread thread1 = mockProcessorThread(1);
        ProcessorThread thread2 = mockProcessorThread(2);

        defaultProcessorScheduler.schedule(1, timeProvider.getMillis() + 10);


        blockTestUntilDaemonThreadSettled();
        assertSleepingWithTimeout(10);

        // just 1 ms passed
        timeProvider.increase(1);
        defaultProcessorScheduler.schedule(2, timeProvider.getMillis() + 3);

        blockTestUntilDaemonThreadSettled();
        assertSleepingWithTimeout(3);

        timeProvider.increase(3);
        sleeper.wakedByTimeout();

        blockTestUntilDaemonThreadSettled();
        assertSleepingWithTimeout(6);
        Mockito.verify(thread2).wakeProcessor();

        timeProvider.increase(7);
        sleeper.wakedByTimeout();

        blockTestUntilDaemonThreadSettled();
        assertSleepingWithoutTimeout();
        Mockito.verify(thread1).wakeProcessor();
    }

    @Test(timeout = 10000)
    public void scheduleMultipleOnSameMillis() throws Exception {
        ProcessorThread thread1 = mockProcessorThread(1);
        ProcessorThread thread2 = mockProcessorThread(2);
        ProcessorThread thread3 = mockProcessorThread(3);
        DefaultProcessorScheduler internalSchedulerExposed = (DefaultProcessorScheduler) defaultProcessorScheduler;

        defaultProcessorScheduler.schedule(1, timeProvider.getMillis() + 10);
        defaultProcessorScheduler.schedule(2, timeProvider.getMillis() + 7);
        defaultProcessorScheduler.schedule(3, timeProvider.getMillis() + 10);
        defaultProcessorScheduler.schedule(1, timeProvider.getMillis() + 10);
        defaultProcessorScheduler.schedule(1, timeProvider.getMillis() + 30);
        defaultProcessorScheduler.schedule(2, timeProvider.getMillis() + 25);
        defaultProcessorScheduler.schedule(1, timeProvider.getMillis() + 15);
        defaultProcessorScheduler.schedule(1, timeProvider.getMillis() + 57);

        blockTestUntilDaemonThreadSettled();
        assertEquals("[7ms : 2, 10ms : 1,3, 15ms : 1, 25ms : 2, 30ms : 1, 57ms : 1]", internalSchedulerExposed.timelineAsLimitedString(timeProvider.getMillis()));
        assertSleepingWithTimeout(7);

        // For some reason, we woke up very late => everything should get notified
        timeProvider.increase(60);
        sleeper.wakedByTimeout();
        blockTestUntilDaemonThreadSettled();
        assertSleepingWithoutTimeout();
        Mockito.verify(thread1).wakeProcessor();
        Mockito.verify(thread2).wakeProcessor();
        Mockito.verify(thread3).wakeProcessor();
    }

    @Test(timeout = 10000)
    public void scheduleInPast() throws Exception {
        ProcessorThread thread1 = mockProcessorThread(1);
        timeProvider.set(100);
        defaultProcessorScheduler.schedule(1, 95);

        blockTestUntilDaemonThreadSettled();
        assertSleepingWithoutTimeout(); // no more schedules - just sleeping
        Mockito.verify(thread1).wakeProcessor();
    }


    ProcessorThread mockProcessorThread(long id) {
        ProcessorThread thread = Mockito.mock(ProcessorThread.class);
        defaultProcessorScheduler.registerThread(id, thread);
        return thread;
    }

    private void assertSleepingWithTimeout(int sleepingMillis) {
        assertTrue(sleeper.isSleeping());
        assertTrue(sleeper.isTimeoutSet());
        assertEquals(timeProvider.getMillis() + sleepingMillis, sleeper.getTimeToWakeMillis());
    }

    private void assertSleepingWithoutTimeout() {
        assertTrue(sleeper.isSleeping());
        assertFalse(sleeper.isTimeoutSet());
    }

    /**
     * In the tests we need to synchronize test thread and daemon thread
     * It's not straightforward, but daemon thread will always result in sleeping
     * It will notify all processors with passed timestamp and then sleep, one way or another.
     * So, if we need to assert any conditions in the tests, let's block test thread, until daemon thread done it's logic
     */

    private void blockTestUntilDaemonThreadSettled() {
        while (true) {
            if (sleeper.isSleeping()) {
                return;
            }
            Thread.yield();
        }
    }

    static class MockedTimeProvider implements ProcessorTimeProvider {

        private final AtomicLong currentMockedTimeMillis;

        MockedTimeProvider(long randomInitMillis) {
            this.currentMockedTimeMillis = new AtomicLong(randomInitMillis);
        }

        @Override
        public long getMillis() {
            return currentMockedTimeMillis.get();
        }

        void increase(long deltaMillis) {
            currentMockedTimeMillis.addAndGet(deltaMillis);
        }

        public void set(int timeMillis) {
            currentMockedTimeMillis.set(timeMillis);
        }
    }

    static class MockedThreadSleeper implements ThreadSleeper {
        private final ProcessorTimeProvider timeProvider;

        private boolean sleeping = false;
        private long timeToWakeMillis = -1;

        MockedThreadSleeper(ProcessorTimeProvider timeProvider) {
            this.timeProvider = timeProvider;
        }

        @Override
        public synchronized void doWait() throws InterruptedException {
            sleeping = true;
            timeToWakeMillis = -1;
            this.wait();
        }

        @Override
        public synchronized void doWaitWithTimeout(long sleepMillis) throws InterruptedException {
            sleeping = true;
            timeToWakeMillis = timeProvider.getMillis() + sleepMillis;
            this.wait();
        }

        @Override
        public synchronized void doNotify() {
            sleeping = false;
            timeToWakeMillis = -1;
            this.notify();
        }

        public synchronized void wakedByTimeout() {
            doNotify();
        }

        synchronized boolean isSleeping() {
            return sleeping;
        }

        synchronized public boolean isTimeoutSet() {
            return timeToWakeMillis != -1;
        }

        public synchronized long getTimeToWakeMillis() {
            return timeToWakeMillis;
        }
    }

}