package com.sequenceiq.cloudbreak.cloud.task;

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

public abstract class PollPredicateStateTask<T> extends AbstractPollTask<T> {

    private final Predicate<? super T> predicate;

    protected PollPredicateStateTask(AuthenticatedContext authenticatedContext, boolean cancellable, Predicate<? super T> predicate) {
        super(requireNonNull(authenticatedContext), cancellable);
        this.predicate = requireNonNull(predicate);
    }

    @Override
    public boolean completed(T t) {
        return predicate.test(t);
    }

}
