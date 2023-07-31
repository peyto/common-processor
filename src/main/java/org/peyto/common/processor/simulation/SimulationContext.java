package org.peyto.common.processor.simulation;

import org.peyto.common.processor.ProcessorContext;

public class SimulationContext implements ProcessorContext {

    @Override
    public void scheduleWakeup(long timeMillis) {

    }

    @Override
    public void cancelAllScheduledWakeups() {

    }

    @Override
    public long getCycleTimeMillis() {
        return 0;
    }

    @Override
    public long processorEndTimeMillis() {
        return 0;
    }
}
