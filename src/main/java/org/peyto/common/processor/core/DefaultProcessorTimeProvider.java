package org.peyto.common.processor.core;

import org.peyto.common.processor.ProcessorTimeProvider;
import org.springframework.stereotype.Service;

@Service
public class DefaultProcessorTimeProvider implements ProcessorTimeProvider {

    @Override
    public long getMillis() {
        return System.currentTimeMillis();
    }
}
