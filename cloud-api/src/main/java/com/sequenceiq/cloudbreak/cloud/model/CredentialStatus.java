package com.sequenceiq.cloudbreak.cloud.model;

public enum CredentialStatus {

    CREATED(),
    PERMISSIONS_MISSING(),
    VERIFIED(),
    DELETED(),
    UPDATED(),
    FAILED();

    private final StatusGroup statusGroup;

    CredentialStatus() {
        statusGroup = StatusGroup.PERMANENT;
    }

    public StatusGroup getStatusGroup() {
        return statusGroup;
    }
}
