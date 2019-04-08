package com.sequenceiq.cloudbreak.cluster.status;

public class ClusterStatusResult {

    private final ClusterStatus clusterStatus;

    private final String statusReasonArg;

    private ClusterStatusResult(ClusterStatus clusterStatus) {
        this(clusterStatus, null);
    }

    public ClusterStatusResult(ClusterStatus clusterStatus, String statusReasonArg) {
        this.clusterStatus = clusterStatus;
        this.statusReasonArg = statusReasonArg;
    }

    public static ClusterStatusResult of(ClusterStatus clusterStatus) {
        return new ClusterStatusResult(clusterStatus);
    }

    public ClusterStatus getClusterStatus() {
        return clusterStatus;
    }

    public String getStatusReason() {
        return statusReasonArg != null
                ? String.format(clusterStatus.getStatusReason(), statusReasonArg)
                : clusterStatus.getStatusReason();
    }
}
