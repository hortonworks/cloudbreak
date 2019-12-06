package com.sequenceiq.cloudbreak.auth;

@FunctionalInterface
public interface ThrowableCallable<V, W extends Throwable> {
    V call() throws W;
}
