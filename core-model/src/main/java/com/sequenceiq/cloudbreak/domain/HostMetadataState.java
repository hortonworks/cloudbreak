package com.sequenceiq.cloudbreak.domain;

public enum HostMetadataState {
    HEALTHY("HEALTY"),
    UNHEALTHY("UNHEALTHY");

    private final String value;

    HostMetadataState(String value) {
        this.value = value;
    }
}
