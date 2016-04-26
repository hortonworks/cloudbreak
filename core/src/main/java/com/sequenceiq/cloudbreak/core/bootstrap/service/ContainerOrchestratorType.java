package com.sequenceiq.cloudbreak.core.bootstrap.service;

public enum ContainerOrchestratorType {
    HOST, CONTAINER;

    public boolean hostOrchestrator() {
        return equals(HOST);
    }

    public boolean containerOrchestrator() {
        return equals(CONTAINER);
    }
}
