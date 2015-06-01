package com.sequenceiq.cloudbreak.cloud.openstack.status;

import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public enum HeatStackStatus {

    CREATED("CREATE_COMPLETE"),
    DELETED("DELETE_COMPLETE"),
    UPDATED("UPDATE_COMPLETE"),
    FAILED("FAILED");

    private final String status;

    private HeatStackStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }


    public ResourceStatus mapResourceStatus(String status) {
        switch (status) {
            case "CREATE_COMPLETE":
                return ResourceStatus.CREATED;
            default:


        }
        return ResourceStatus.IN_PROGRESS;
    }
}
