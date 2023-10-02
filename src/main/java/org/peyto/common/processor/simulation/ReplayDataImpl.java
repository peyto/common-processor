package org.peyto.common.processor.simulation;

import java.util.List;

public class ReplayDataImpl<T extends Object> implements ReplayData<T> {

    private final long threadId;
    private final T configurationObject;
    private final List<CycleData> replayData;

    public ReplayDataImpl(long threadId, T configurationObject, List<CycleData> replayData) {
        this.threadId = threadId;
        this.configurationObject = configurationObject;
        this.replayData = replayData;
    }

    @Override
    public long getThreadId() {
        return threadId;
    }

    @Override
    public T getConfigurationObject() {
        return configurationObject;
    }

    @Override
    public List<CycleData> getCycleData() {
        return replayData;
    }
}
