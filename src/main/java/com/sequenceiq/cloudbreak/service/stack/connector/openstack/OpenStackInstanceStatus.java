package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

public enum OpenStackInstanceStatus {

    STARTED("ACTIVE"),
    STOPPED("SHUTOFF");

    private final String status;

    private OpenStackInstanceStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
