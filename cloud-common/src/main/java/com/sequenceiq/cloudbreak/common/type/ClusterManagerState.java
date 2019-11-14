package com.sequenceiq.cloudbreak.common.type;

public class ClusterManagerState {

    private String statusReason;

    private ClusterManagerStatus clusterManagerStatus;

    public enum ClusterManagerStatus {
        HEALTHY, UNHEALTHY
    }

    public ClusterManagerState(ClusterManagerStatus clusterManagerStatus, String statusReason) {
        this.clusterManagerStatus = clusterManagerStatus;
        this.statusReason = statusReason;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public ClusterManagerStatus getClusterManagerStatus() {
        return clusterManagerStatus;
    }

    public void setClusterManagerStatus(ClusterManagerStatus clusterManagerStatus) {
        this.clusterManagerStatus = clusterManagerStatus;
    }
}
