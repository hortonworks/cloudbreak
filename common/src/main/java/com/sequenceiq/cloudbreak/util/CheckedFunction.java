package com.sequenceiq.cloudbreak.util;

@FunctionalInterface
public interface CheckedFunction<T, R, E extends Exception> {

    R apply(T t) throws E;
}
