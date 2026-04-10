package com.sequenceiq.cloudbreak.rotation.common;

public interface ConditionalRotationContextProvider<E> extends RotationContextProvider  {

    default boolean isApplicable(E entity) {
        return true;
    }
}
