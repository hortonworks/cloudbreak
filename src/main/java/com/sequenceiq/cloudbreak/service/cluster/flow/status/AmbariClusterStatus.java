package com.sequenceiq.cloudbreak.service.cluster.flow.status;

import com.sequenceiq.cloudbreak.domain.Status;

public class AmbariClusterStatus {
    private ClusterStatus status;
    private Status stackStatus;
    private Status clusterStatus;
    private String statusReason;

    public AmbariClusterStatus(ClusterStatus status, Status stackStatus, Status clusterStatus, String statusReason) {
        this.status = status;
        this.stackStatus = stackStatus;
        this.clusterStatus = clusterStatus;
        this.statusReason = statusReason;
    }

    public ClusterStatus getStatus() {
        return status;
    }

    public Status getStackStatus() {
        return stackStatus;
    }

    public Status getClusterStatus() {
        return clusterStatus;
    }

    public String getStatusReason() {
        return statusReason;
    }
}
