package com.sequenceiq.cloudbreak.cloud.openstack.status;

import org.openstack4j.model.compute.Server;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

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

    public static InstanceStatus get(Server server) {
        String status = server.getStatus().toString();
        if (status.equals(STOPPED.getStatus())) {
            return InstanceStatus.STOPPED;
        } else if (status.equals(STARTED.getStatus())) {
            return InstanceStatus.STARTED;
        } else {
            return InstanceStatus.IN_PROGRESS;
        }
    }
}
