package org.peyto.common.processor;

public interface ProcessorTimeProvider {

    /**
     * Return the current time, used in statistics measurement
     * The simpliest implementation is just System.getCurrentTimeMillis(). But for replay/tests we might
     * want to inject some replay mechanism to receive deterministic results of event replays.
     * @return current time millis
     */
    long getMillis();

}
