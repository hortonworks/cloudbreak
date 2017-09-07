package com.sequenceiq.cloudbreak.orchestrator.yarn.api;

public enum Entrypoint {
    AMBARIDB("/docker-entrypoint"),
    AMBARISERVER("/docker-entrypoint"),
    AMBARIAGENT("/docker-entrypoint");

    private final String entryPoint;

    Entrypoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public String getEntryPoint() {
        return entryPoint;
    }
}
