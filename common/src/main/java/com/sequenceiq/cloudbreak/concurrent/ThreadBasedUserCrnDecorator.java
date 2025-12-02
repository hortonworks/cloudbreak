package com.sequenceiq.cloudbreak.concurrent;

import java.util.concurrent.Callable;
import java.util.function.Function;

import org.springframework.core.task.TaskDecorator;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

public class ThreadBasedUserCrnDecorator implements TaskDecorator, Function<Callable<?>, Callable<?>> {

    @Override
    public Runnable decorate(Runnable runnable) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return () -> ThreadBasedUserCrnProvider.doAs(userCrn, runnable);
    }

    @Override
    public Callable<?> apply(Callable<?> callable) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return () -> ThreadBasedUserCrnProvider.doAsCallable(userCrn, callable);
    }
}
