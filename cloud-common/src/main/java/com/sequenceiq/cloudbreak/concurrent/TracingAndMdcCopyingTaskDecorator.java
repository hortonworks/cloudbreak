package com.sequenceiq.cloudbreak.concurrent;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

public class TracingAndMdcCopyingTaskDecorator implements TaskDecorator {

    private final Tracer tracer;

    public TracingAndMdcCopyingTaskDecorator(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Runnable decorate(Runnable runnable) {

        // Originating thread, where the executor is called from 2
        final Span activeSpan = tracer.activeSpan();
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();

        return () -> {
            // Thread of the executed task
            MDC.setContextMap(copyOfContextMap);
            try (Scope ignored = tracer.activateSpan(activeSpan)) {
                runnable.run();
            }
            MDCBuilder.cleanupMdc();
        };
    }
}
