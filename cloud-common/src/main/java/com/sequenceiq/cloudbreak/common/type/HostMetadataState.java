package com.sequenceiq.cloudbreak.common.type;

public enum HostMetadataState {
    CONTAINER_RUNNING("CONTAINER_RUNNING"),
    HEALTHY("HEALTY"),
    UNHEALTHY("UNHEALTHY");

    private final String value;

    HostMetadataState(String value) {
        this.value = value;
    }
}
