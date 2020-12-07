package com.sequenceiq.cloudbreak.util;

@FunctionalInterface
public interface CheckedSupplier<R, E extends Throwable> {

    R get() throws E;
}
