package com.sequenceiq.cloudbreak.cluster.status;

import java.util.Arrays;
import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;

public enum ClusterStatus {
    UNKNOWN(null, null, "Error happened during the communication with Ambari"),
    AMBARISERVER_NOT_RUNNING(null, null, "Ambariserver is not running."),
    AMBARISERVER_RUNNING(Status.AVAILABLE, null, "Ambari server is running."),
    INSTALLING(Status.AVAILABLE, null, "Ambari server is running, services are being installed... [%s]"),
    INSTALLED(Status.AVAILABLE, Status.STOPPED, "Services are installed but not running."),
    INSTALL_FAILED(Status.AVAILABLE, null, "Ambari server is running, but the ambari installation has failed. [%s]"),
    STARTING(Status.AVAILABLE, Status.START_IN_PROGRESS, "Services are installed, starting... [%s]"),
    STARTED(Status.AVAILABLE, Status.AVAILABLE, "Services are installed and running."),
    STOPPING(Status.AVAILABLE, Status.STOP_IN_PROGRESS, "Services are installed, stopping... [%s]"),
    PENDING(Status.AVAILABLE, null, "There are in progress or pending operations in Ambari. Wait them to be finsihed and try syncing later."),
    AMBIGUOUS(Status.AVAILABLE, Status.AVAILABLE,
            "There are stopped and running Ambari services as well. [%s] Restart or stop all of them and try syncing later.");

    private final String statusReason;

    private String statusReasonArg;

    private final Status stackStatus;

    private final Status clusterStatus;

    ClusterStatus(Status stackStatus, Status clusterStatus, String statusReason) {
        this.stackStatus = stackStatus;
        this.clusterStatus = clusterStatus;
        this.statusReason = statusReason;
        this.statusReasonArg = "";
    }

    public String getStatusReason() {
        return String.format(statusReason, statusReasonArg);
    }

    public void setStatusReasonArg(String statusReasonArg) {
        this.statusReasonArg = Optional.ofNullable(statusReasonArg).orElse("");
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
