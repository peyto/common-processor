package org.peyto.common.processor.simulation;

import org.peyto.common.processor.ProcessorThreadListener;
import org.peyto.common.processor.ProcessorTimeProvider;
import org.peyto.common.processor.core.InternalProcessorContext;
import org.peyto.common.processor.utils.CheckerUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationContext implements InternalProcessorContext, ProcessorTimeProvider, ProcessorThreadListener {

    private final List<ReplayData.CycleData> cycleData;
    private final long endTimeMillis;

    private final AtomicInteger currentArrayStep = new AtomicInteger(0);

    private ReplayData.CycleData currentStep = null;

    public SimulationContext(List<ReplayData.CycleData> cycleData) {
        CheckerUtils.checkArg(cycleData != null && cycleData.size() > 0, "Expected at least 1 cycle execution for replay data");
        this.cycleData = cycleData;
        this.endTimeMillis = cycleData.stream().map(ReplayData.CycleData::getTimestampMillis).max(Long::compareTo).get();
    }

    @Override
    public long calculateNextCycleNumber() {
        int index = currentArrayStep.getAndIncrement();
        currentStep = cycleData.get(index);
        return currentStep.getCycleNumber();
    }

    @Override
    public long calculateNextCycleTime() {
        int index = currentArrayStep.getAndIncrement();
        currentStep = cycleData.get(index);
        return currentStep.getTimestampMillis();
    }

    @Override
    public long getCycleNumber() {
        return currentStep.getCycleNumber();
    }

    @Override
    public long getCycleTimeMillis() {
        return currentStep.getTimestampMillis();
    }

    @Override
    public long processorEndTimeMillis() {
        return endTimeMillis;
    }


    @Override
    public long getMillis() {
        return currentStep.getTimestampMillis();
    }

    @Override
    public void scheduleWakeup(long timeMillis) {
    }

    @Override
    public void cancelAllScheduledWakeups() {
    }

    @Override
    public void onFinish(long threadId) {
    }
}
