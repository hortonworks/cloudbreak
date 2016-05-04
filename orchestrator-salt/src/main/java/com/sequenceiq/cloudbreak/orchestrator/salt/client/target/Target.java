package com.sequenceiq.cloudbreak.orchestrator.salt.client.target;

public interface Target<T> {

    T getTarget();

    String getType();
}
