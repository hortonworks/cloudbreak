package com.sequenceiq.cloudbreak.core.flow2;

public interface EventConverter<E> {
    E convert(String key);
}
