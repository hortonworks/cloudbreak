package com.sequenceiq.flow.core;

public interface StateConverter<S> {
    S convert(String stateRepresentation);
}
