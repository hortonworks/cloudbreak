package com.sequenceiq.cloudbreak.orchestrator.yarn.api;

public enum Entrypoint {
    AMBARIDB("/docker-entrypoint.sh"),
    AMBARISERVER("/tmp/privileged-init"),
    AMBARIAGENT("/tmp/privileged-init");

    private String entryPoint;

    Entrypoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public String getEntryPoint() {
        return entryPoint;
    }
}
