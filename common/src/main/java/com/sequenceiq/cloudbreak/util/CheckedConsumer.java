package com.sequenceiq.cloudbreak.util;

@FunctionalInterface
public interface CheckedConsumer<T, E extends Throwable> {

    void accept(T t) throws E;
}
