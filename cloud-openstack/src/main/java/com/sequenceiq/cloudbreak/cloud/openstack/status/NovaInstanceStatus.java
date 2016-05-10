package com.sequenceiq.cloudbreak.cloud.openstack.status;

import org.openstack4j.model.compute.Server;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public enum NovaInstanceStatus {

    STARTED("ACTIVE"),
    STOPPED("SHUTOFF"),
    SUSPENDED("SUSPENDED"),
    PAUSED("PAUSED");

    private final String status;

    NovaInstanceStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public static InstanceStatus get(Server server) {
        String status = server.getStatus().toString();
        if (isStoppedInstanceStatus(status)) {
            return InstanceStatus.STOPPED;
        } else if (status.equals(STARTED.getStatus())) {
            return InstanceStatus.STARTED;
        } else {
            return InstanceStatus.IN_PROGRESS;
        }
    }

    private static boolean isStoppedInstanceStatus(String status) {
        return status.equals(STOPPED.getStatus()) || status.equals(SUSPENDED.getStatus()) || status.equals(PAUSED.getStatus());
    }
}
