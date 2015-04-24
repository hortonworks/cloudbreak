package com.sequenceiq.cloudbreak.service.cluster.flow.status;

public enum ClusterStatus {
    UNKNOWN,
    AMBARISERVER_NOT_RUNNING,
    AMBARISERVER_RUNNING,
    INSTALLING,
    INSTALLED,
    INSTALL_FAILED,
    STARTING,
    STARTED,
    STOPPING
}
