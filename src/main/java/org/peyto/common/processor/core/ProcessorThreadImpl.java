package org.peyto.common.processor.core;

import org.peyto.common.processor.*;
import org.peyto.common.processor.core.schedule.ProcessorScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.peyto.common.processor.utils.CheckerUtils.checkArg;

/**
 * Generic Processor Thread, which is logic-agnostic, but have number of features:
 * The class is not public, the user should use ProcessorThreadFactory
 * 1. Each processor thread is runnable in a separate thread
 * 2. Business logic should implement Processor interface, representing logic of a single cycle
 * 3. Each Processor cycle should have deterministic behaviour, based on the state, current cycle timestamp and inputs
 * 4. Processor thread can have inputs
 * 5. If the processor cycle resulted in a BUSY, it will be runned again. Otherwise it will sleep
 * 6. The processor thread can be wake up by either input or scheduled wakeup timestamp
 * @param <T> class of configuration object
 */
class ProcessorThreadImpl<T> extends Thread implements ProcessorThread, ProcessorProviderBinder {

    private static final Logger log = LoggerFactory.getLogger(ProcessorThreadImpl.class);
    private final long customThreadId;
    private final ProcessorScheduler processorScheduler;
    private final ProcessorThreadListener threadStatusChangeListener;

    private final Processor processor;
    private final InternalProcessorContext context;


    private final Object syncObj = new Object();
    private final ArrayList<QueueReceiver<Object>> inputs = new ArrayList<>();

    private final AtomicBoolean stoppingProcessor = new AtomicBoolean(false);

    /**
     *
     * @param processorProvider provider of processor
     * @param threadId long unique id of the processor thread
     * @param configurationObject configuration object to build processor (will be passed to processor provider)
     * @param processorScheduler
     * @param processorTimeProvider
     * @param processorEndTimeMillis
     */
    public ProcessorThreadImpl(ProcessorProvider<T> processorProvider,
                               ThreadGroup parentThreadGroup,
                               Long threadId,
                               T configurationObject,
                               ProcessorTimeProvider processorTimeProvider,
                               ProcessorScheduler processorScheduler,
                               ProcessorThreadListener threadStatusChangeListener,
                               long processorEndTimeMillis) {
        super(parentThreadGroup, "processor-"+threadId);
        this.customThreadId = threadId != null ? threadId : super.getId();
        processorScheduler.registerThread(customThreadId, this);
        this.processor = processorProvider.get(configurationObject, this);
        this.context = new ProcessorContextImpl(customThreadId, processorScheduler, processorTimeProvider, processorEndTimeMillis);
        this.threadStatusChangeListener = threadStatusChangeListener;
        this.processorScheduler = processorScheduler;
    }

    public ProcessorThreadImpl(ProcessorProvider<T> processorProvider,
                               ThreadGroup parentThreadGroup,
                               Long threadId,
                               T configurationObject,
                               InternalProcessorContext processorContext,
                               ProcessorScheduler processorScheduler,
                               ProcessorThreadListener threadStatusChangeListener) {
        super(parentThreadGroup, "processor-"+threadId);
        this.customThreadId = threadId != null ? threadId : super.getId();
        processorScheduler.registerThread(customThreadId, this);
        this.processor = processorProvider.get(configurationObject, this);
        this.context = processorContext;
        this.threadStatusChangeListener = threadStatusChangeListener;
        this.processorScheduler = processorScheduler;
    }

    @Override
    public void onInput(int index, Object input) {
        checkArg(index < inputs.size(), "Received input %d, but only registered %d", index, inputs.size());
        synchronized (syncObj) {
            inputs.get(index).offer(input);
            syncObj.notify();
        }
    }

    @Override
    public void wakeProcessor() {
        synchronized (syncObj) {
            syncObj.notify();
        }
    }

    @Override
    public void run() {
        processor.init(context);
        while (context.calculateNextCycleTime() <= context.processorEndTimeMillis() && !stoppingProcessor.get()) {
            try {
                context.calculateNextCycleNumber();
                ProcessorResult result = processor.process(context);
                if (result == ProcessorResult.END) {
                    log.info("Processor task {} is finishing", customThreadId);
                    stoppingProcessor.set(true);
                } else if (result == ProcessorResult.IDLE) {
                    synchronized (syncObj) {
                        syncObj.wait();
                    }
                }
            } catch (Exception e) {
                log.error("Processor error", e);
                processor.handleProcessorException(e);
            }
        }
        try {
            processor.end(context);
        } catch (Exception e) {
            log.error("There was an error stopping processor", e);
        }

        // Remove from scheduler, etc
        processorScheduler.onFinish(customThreadId);
        threadStatusChangeListener.onFinish(customThreadId);
        log.info("The processor task {} has stopped", customThreadId);
    }

    @Override
    public <T> Receiver<T> registerInput(int index) {
        QueueReceiver queueEndpoint = new QueueReceiver<>();
        inputs.add(index, queueEndpoint);
        return queueEndpoint;
    }

    @Override
    public long getCustomId() {
        return customThreadId;
    }

    @Override
    public Object getProcessorState(Object... request) {
        return processor.exposeProcessorState(request);
    }
}