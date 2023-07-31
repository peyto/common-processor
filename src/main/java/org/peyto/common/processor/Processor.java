package org.peyto.common.processor;

public interface Processor {

    default void init(ProcessorContext context) {
    }

    ProcessorResult process(ProcessorContext context);

    default void end(ProcessorContext context) {
    }

    void handleProcessorException(Exception e);

    default Object exposeProcessorState(Object... request) {
        return null;
    }
}
