package com.sequenceiq.common.model;

public enum CloudIdentityType {
    ID_BROKER("ID_BROKER_CLOUD_IDENTITY_ROLE"), LOG("LOG_CLOUD_IDENTITY_ROLE");

    private final String roleName;

    CloudIdentityType(String roleName) {
        this.roleName = roleName;
    }

    public String roleName() {
        return roleName;
    }
}
