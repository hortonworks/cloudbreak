package com.sequenceiq.cloudbreak.concurrent;

import static java.util.Objects.requireNonNull;

import org.springframework.core.task.TaskDecorator;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

/**
 * Task decorator that propagates the actor CRN from the caller to
 * the running task.
 */
public class ActorCrnTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        requireNonNull(runnable, "runnable is null");
        String actorCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return () -> {
            ThreadBasedUserCrnProvider.doAs(actorCrn, runnable);
        };
    }
}
