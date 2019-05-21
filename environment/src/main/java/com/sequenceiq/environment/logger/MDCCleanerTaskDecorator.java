package com.sequenceiq.environment.logger;

import org.springframework.core.task.TaskDecorator;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class MDCCleanerTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return () -> {
            runnable.run();
            MDCBuilder.cleanupMdc();
        };
    }
}
