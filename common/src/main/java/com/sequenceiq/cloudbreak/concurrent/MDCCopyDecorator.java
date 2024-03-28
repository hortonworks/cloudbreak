package com.sequenceiq.cloudbreak.concurrent;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class MDCCopyDecorator implements TaskDecorator, Function<Callable, Callable> {

    @Override
    public Runnable decorate(Runnable runnable) {

        // Originating thread, where the executor is called from 2
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        return () -> {
            // Thread of the executed task
            try {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                runnable.run();
            } finally {
                MDCBuilder.cleanupMdc();
            }
        };
    }

    @Override
    public Callable apply(Callable callable) {
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                return callable.call();
            } finally {
                MDCBuilder.cleanupMdc();
            }
        };
    }
}
