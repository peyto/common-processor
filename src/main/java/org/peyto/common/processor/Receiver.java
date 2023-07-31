package org.peyto.common.processor;

public interface Receiver<T> {

    T receive();

    boolean hasData();

    void clear();
}
