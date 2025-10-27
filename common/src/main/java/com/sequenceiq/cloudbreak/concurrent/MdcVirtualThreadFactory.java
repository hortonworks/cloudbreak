package com.sequenceiq.cloudbreak.concurrent;

import java.util.Map;
import java.util.concurrent.ThreadFactory;

import org.slf4j.MDC;

/**
 * A ThreadFactory that creates virtual threads, automatically propagating
 * the MDC context from the thread submitting the task to the new virtual thread.
 */
public class MdcVirtualThreadFactory implements ThreadFactory {

    private final ThreadFactory delegateFactory = Thread.ofVirtual().factory();

    @Override
    public Thread newThread(Runnable r) {
        final Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        Runnable mdcWrapper = () -> {
            if (mdcContext != null) {
                MDC.setContextMap(mdcContext);
            }
            try {
                r.run();
            } finally {
                MDC.clear();
            }
        };

        return delegateFactory.newThread(mdcWrapper);
    }
}
