package com.sequenceiq.cloudbreak.orchestrator.salt.client;

public enum SaltClientType {

    LOCAL("local"),
    RUNNER("runner"),
    LOCAL_ASYNC("local_async");

    private final String type;

    SaltClientType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
