package org.peyto.common.processor.core;

import org.peyto.common.processor.ProcessorContext;

public interface InternalProcessorContext extends ProcessorContext {

    long calculateNextCycleNumber();

    long calculateNextCycleTime();
}
