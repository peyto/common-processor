package org.peyto.common.processor.core;

import org.peyto.common.processor.Receiver;

import java.util.ArrayDeque;
import java.util.Queue;

public class QueueReceiver<T> implements Receiver<T> {

    private final Queue<T> underlyingQueue = new ArrayDeque<>();

    @Override
    public synchronized T receive() {
        return underlyingQueue.poll();
    }

    @Override
    public synchronized boolean hasData() {
        return underlyingQueue.size() > 0;
    }

    @Override
    public synchronized void clear() {
        underlyingQueue.clear();
    }

    public synchronized void offer(T obj) {
        underlyingQueue.offer(obj);
    }
}
