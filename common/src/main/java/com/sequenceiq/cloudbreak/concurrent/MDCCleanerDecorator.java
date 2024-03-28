package com.sequenceiq.cloudbreak.concurrent;

import java.util.concurrent.Callable;
import java.util.function.Function;

import org.springframework.core.task.TaskDecorator;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class MDCCleanerDecorator implements Function<Callable, Callable>, TaskDecorator {

    @Override
    public Callable apply(Callable callable) {
        return () -> {
            try {
                return callable.call();
            } finally {
                MDCBuilder.cleanupMdc();
            }
        };
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } finally {
                MDCBuilder.cleanupMdc();
            }
        };
    }
}
