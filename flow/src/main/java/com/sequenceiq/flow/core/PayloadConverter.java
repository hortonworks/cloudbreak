package com.sequenceiq.flow.core;

public interface PayloadConverter<P> {
    boolean canConvert(Class<?> sourceClass);

    P convert(Object payload);
}
