package com.sequenceiq.cloudbreak.concurrent;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class MDCCleanerTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        return () -> {
            MDC.setContextMap(copyOfContextMap);
            runnable.run();
            MDCBuilder.cleanupMdc();
        };
    }
}
