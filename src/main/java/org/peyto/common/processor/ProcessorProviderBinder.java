package org.peyto.common.processor;

public interface ProcessorProviderBinder {

    <T extends Object> Receiver<T> registerInput(int index);
}
