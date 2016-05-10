package com.sequenceiq.cloudbreak.cloud.model;

public enum CredentialStatus {

    CREATED(StatusGroup.PERMANENT),
    VERIFIED(StatusGroup.PERMANENT),
    DELETED(StatusGroup.PERMANENT),
    UPDATED(StatusGroup.PERMANENT),
    FAILED(StatusGroup.PERMANENT);

    private StatusGroup statusGroup;

    CredentialStatus(StatusGroup statusGroup) {
        this.statusGroup = statusGroup;
    }

    public StatusGroup getStatusGroup() {
        return statusGroup;
    }


}
