package com.sequenceiq.flow.core;

public interface EventConverter<E> {
    E convert(String key);
}
