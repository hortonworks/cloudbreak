package com.sequenceiq.cloudbreak.concurrent;

import static java.util.Objects.requireNonNull;

import java.util.List;

import org.springframework.core.task.TaskDecorator;

import com.google.common.collect.ImmutableList;

/**
 * Task decorator that composes multiple task decorators.
 */
public class CompositeTaskDecorator implements TaskDecorator {

    private final ImmutableList<TaskDecorator> decorators;

    /**
     * Create a CompositeTaskDecorator from the list of task decorators.
     *
     * @param decorators a list of task decorators to apply, from innermost to outermost.
     */
    public CompositeTaskDecorator(List<TaskDecorator> decorators) {
        requireNonNull(decorators, "decorators is null");
        this.decorators = ImmutableList.copyOf(decorators);
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        Runnable decoratedRunnable = requireNonNull(runnable, "runnable is null");
        for (TaskDecorator decorator : decorators) {
            decoratedRunnable = decorator.decorate(decoratedRunnable);
        }
        return decoratedRunnable;
    }
}
