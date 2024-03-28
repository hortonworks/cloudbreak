package com.sequenceiq.cloudbreak.concurrent;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Callable;
import java.util.function.Function;

import org.springframework.core.task.TaskDecorator;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

/**
 * Task decorator that propagates the actor CRN from the caller to
 * the running task.
 */
public class ActorCrnTaskDecorator implements TaskDecorator, Function<Callable, Callable> {
    @Override
    public Runnable decorate(Runnable runnable) {
        requireNonNull(runnable, "runnable is null");
        String actorCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return () -> ThreadBasedUserCrnProvider.doAs(actorCrn, runnable);
    }

    @Override
    public Callable apply(Callable callable) {
        requireNonNull(callable, "callable is null");
        String actorCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return () -> ThreadBasedUserCrnProvider.doAsCallable(actorCrn, callable);
    }
}
