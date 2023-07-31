package org.peyto.common.processor;

public interface ProcessorProvider<T> {

    Processor get(T obj, ProcessorProviderBinder inputBinder);

}
