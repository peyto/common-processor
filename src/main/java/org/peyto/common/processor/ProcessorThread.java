package org.peyto.common.processor;

public interface ProcessorThread extends Runnable {

    void start();

    void onInput(int number, Object input);

    void wakeProcessor();

    /**
     * The reason it's custom - because there is id in Thread
     * But we want to have specific id, so we can have more control over it
     *
     * @return id of the thread, which was used when creating ProcessorThread. If none was passed - it will correspond to Thread.getId()
     */
    long getCustomId();

    Object getProcessorState(Object... request);
}
