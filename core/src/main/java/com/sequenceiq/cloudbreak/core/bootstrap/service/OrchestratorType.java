package com.sequenceiq.cloudbreak.core.bootstrap.service;

public enum OrchestratorType {
    HOST, CONTAINER;

    public boolean hostOrchestrator() {
        return equals(HOST);
    }

    public boolean containerOrchestrator() {
        return equals(CONTAINER);
    }
}
