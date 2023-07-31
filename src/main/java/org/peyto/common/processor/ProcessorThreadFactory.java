package org.peyto.common.processor;

import org.peyto.common.processor.simulation.ManualSimulationController;
import org.peyto.common.processor.simulation.ReplayData;

public interface ProcessorThreadFactory {

    <T> ProcessorThread createProcessorThread(
            ProcessorProvider<T> processorProvider,
            Long threadId,
            T configurationObject,
            ProcessorThreadListener threadStatusChangeListener,
            long processorEndTimeMillis
    );

    <T> ProcessorThread createSimulationReplay(
            ProcessorProvider<T> processorProvider,
            long threadId,
            T configurationObject,
            ProcessorThreadListener threadStatusChangeListener,
            ReplayData replayData
    );

    <T> ProcessorThread createSimulationManual(
            ProcessorProvider<T> processorProvider,
            long threadId,
            T configurationObject,
            ProcessorThreadListener threadStatusChangeListener,
            ManualSimulationController manualSimulationController
    );
}