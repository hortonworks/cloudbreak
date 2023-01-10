package com.sequenceiq.cloudbreak.concurrent;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class MdcCopyingTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {

        // Originating thread, where the executor is called from 2
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();

        return () -> {
            // Thread of the executed task
            MDC.setContextMap(copyOfContextMap);
            runnable.run();
            MDCBuilder.cleanupMdc();
        };
    }
}
