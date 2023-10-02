package org.peyto.common.processor;

public interface Processor {

    ProcessorResult process(ProcessorContext context);

    default void init(ProcessorContext context) {
    }

    default void end(ProcessorContext context) {
    }

    void handleProcessorException(Exception e);

    default Object exposeProcessorState(Object... requestArgs) {
        return null;
    }
}
