package com.sequenceiq.cloudbreak.auth;

@FunctionalInterface
public interface ThrowableRunnable<E extends Throwable> {

    void run() throws E;
}
