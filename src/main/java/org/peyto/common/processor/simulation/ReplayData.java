package org.peyto.common.processor.simulation;

import java.util.List;

public interface ReplayData<T extends Object> {

    long getThreadId();

    T getConfigurationObject();

    List<CycleData> getCycleData();

    class CycleData {

        private final long cycleNumber;
        private final long timestampMillis;
        private final Object[] inputs;

        public CycleData(long cycleNumber, long timestampMillis, Object[] inputs) {
            this.cycleNumber = cycleNumber;
            this.timestampMillis = timestampMillis;
            this.inputs = inputs;
        }

        public long getCycleNumber() {
            return cycleNumber;
        }

        public long getTimestampMillis() {
            return timestampMillis;
        }

        public Object[] getInputs() {
            return inputs;
        }
    }
}
