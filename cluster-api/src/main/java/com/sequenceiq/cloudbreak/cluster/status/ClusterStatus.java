package com.sequenceiq.cloudbreak.cluster.status;

import java.util.Arrays;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;

public enum ClusterStatus {
    UNKNOWN(DetailedStackStatus.UNKNOWN, "Error happened during the communication with the Cluster Manager"),
    CLUSTERMANAGER_NOT_RUNNING(DetailedStackStatus.CLUSTER_MANAGER_NOT_RESPONDING, "The Cluster Manager server is not running."),
    CLUSTERMANAGER_RUNNING(DetailedStackStatus.AVAILABLE, "The Cluster Manager server is running."),
    INSTALLING(DetailedStackStatus.STARTING_CLUSTER_MANAGER_SERVICES,
            "The Cluster Manager server is running, services are being installed... [%s]"),
    INSTALLED(DetailedStackStatus.AVAILABLE, "Services are installed but not running."),
    INSTALL_FAILED(DetailedStackStatus.AVAILABLE, "The Cluster Manager server is running, but service installation has failed. [%s]"),
    STARTING(DetailedStackStatus.START_IN_PROGRESS, "Services are installed, starting... [%s]"),
    STARTED(DetailedStackStatus.AVAILABLE, "Services are installed and running."),
    STOPPING(DetailedStackStatus.STOP_IN_PROGRESS, "Services are installed, stopping... [%s]"),
    PENDING(DetailedStackStatus.AVAILABLE,
            "There are in progress or pending operations in the Cluster Manager. Wait them to be finished and try syncing later."),
    AMBIGUOUS(DetailedStackStatus.AVAILABLE,
            "There are both stopped and running services. [%s] Restart or stop all of them and try syncing later.");

    private final String statusReason;

    private final DetailedStackStatus detailedStackStatus;

    ClusterStatus(DetailedStackStatus detailedStackStatus, String statusReason) {
        this.detailedStackStatus = detailedStackStatus;
        this.statusReason = statusReason;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public DetailedStackStatus getDetailedStackStatus() {
        return detailedStackStatus;
    }

    public static boolean supported(String status) {
        return Arrays.stream(values()).map(ClusterStatus::name).anyMatch(status::equalsIgnoreCase);
    }
}
