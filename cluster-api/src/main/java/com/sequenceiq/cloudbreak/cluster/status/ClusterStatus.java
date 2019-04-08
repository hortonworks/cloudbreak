package com.sequenceiq.cloudbreak.cluster.status;

import java.util.Arrays;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;

public enum ClusterStatus {
    UNKNOWN(null, null, "Error happened during the communication with the Cluster Manager"),
    AMBARISERVER_NOT_RUNNING(null, null, "The Cluster Manager server is not running."),
    AMBARISERVER_RUNNING(Status.AVAILABLE, null, "The Cluster Manager server is running."),
    INSTALLING(Status.AVAILABLE, null, "The Cluster Manager server is running, services are being installed... [%s]"),
    INSTALLED(Status.AVAILABLE, Status.STOPPED, "Services are installed but not running."),
    INSTALL_FAILED(Status.AVAILABLE, null, "The Cluster Manager server is running, but service installation has failed. [%s]"),
    STARTING(Status.AVAILABLE, Status.START_IN_PROGRESS, "Services are installed, starting... [%s]"),
    STARTED(Status.AVAILABLE, Status.AVAILABLE, "Services are installed and running."),
    STOPPING(Status.AVAILABLE, Status.STOP_IN_PROGRESS, "Services are installed, stopping... [%s]"),
    PENDING(Status.AVAILABLE, null, "There are in progress or pending operations in the Cluster Manager. Wait them to be finished and try syncing later."),
    AMBIGUOUS(Status.AVAILABLE, Status.AVAILABLE,
            "There are both stopped and running services. [%s] Restart or stop all of them and try syncing later.");

    private final String statusReason;

    private final Status stackStatus;

    private final Status clusterStatus;

    ClusterStatus(Status stackStatus, Status clusterStatus, String statusReason) {
        this.stackStatus = stackStatus;
        this.clusterStatus = clusterStatus;
        this.statusReason = statusReason;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public Status getStackStatus() {
        return stackStatus;
    }

    public Status getClusterStatus() {
        return clusterStatus;
    }

    public static boolean supported(String status) {
        return Arrays.stream(values()).map(ClusterStatus::name).anyMatch(status::equalsIgnoreCase);
    }
}
