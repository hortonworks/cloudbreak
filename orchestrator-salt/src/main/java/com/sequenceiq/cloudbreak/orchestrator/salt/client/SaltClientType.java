package com.sequenceiq.cloudbreak.orchestrator.salt.client;

public enum SaltClientType {

    LOCAL("local"),
    LOCAL_ASYNC("local_async");

    private String type;

    SaltClientType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
