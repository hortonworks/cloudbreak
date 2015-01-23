package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

public enum HeatStackStatus {

    CREATED("CREATE_COMPLETE"),
    DELETED("DELETE_COMPLETE"),
    DELETE_FAILED("DELETE_FAILED"),
    CREATE_FAILED("CREATE_FAILED");

    private final String status;

    private HeatStackStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
