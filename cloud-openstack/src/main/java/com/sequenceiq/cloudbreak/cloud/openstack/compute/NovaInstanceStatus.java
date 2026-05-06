package com.sequenceiq.cloudbreak.cloud.openstack.compute;

import org.openstack4j.model.compute.Server;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public enum NovaInstanceStatus {

    STARTED("ACTIVE"),
    SHUTOFF("SHUTOFF"),
    STOPPED("STOPPED");

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
        } else if (status.equals(STARTED.status)) {
            return InstanceStatus.STARTED;
        } else {
            return InstanceStatus.IN_PROGRESS;
        }
    }

    private static boolean isStoppedInstanceStatus(String status) {
        return status.equals(SHUTOFF.status) || status.equals(STOPPED.status);
    }
}
