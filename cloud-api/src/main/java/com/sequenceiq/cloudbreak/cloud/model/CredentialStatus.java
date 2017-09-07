package com.sequenceiq.cloudbreak.cloud.model;

public enum CredentialStatus {

    CREATED(),
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
