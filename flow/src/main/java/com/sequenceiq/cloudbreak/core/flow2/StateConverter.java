package com.sequenceiq.cloudbreak.core.flow2;

public interface StateConverter<S> {
    S convert(String stateRepresentation);
}
