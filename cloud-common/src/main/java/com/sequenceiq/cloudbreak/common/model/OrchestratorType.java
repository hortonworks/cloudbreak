package com.sequenceiq.cloudbreak.common.model;

public enum OrchestratorType {
    HOST, CONTAINER;

    public boolean hostOrchestrator() {
        return equals(HOST);
    }

    public boolean containerOrchestrator() {
        return equals(CONTAINER);
    }
}
