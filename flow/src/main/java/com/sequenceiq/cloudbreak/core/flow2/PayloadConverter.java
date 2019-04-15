package com.sequenceiq.cloudbreak.core.flow2;

public interface PayloadConverter<P> {
    boolean canConvert(Class<?> sourceClass);

    P convert(Object payload);
}
