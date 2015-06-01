package com.sequenceiq.cloudbreak.cloud.openstack.status;

public enum NovaInstanceStatus {

    STARTED("ACTIVE"),
    STOPPED("SHUTOFF");

    private final String status;

    private NovaInstanceStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
