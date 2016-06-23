package com.sequenceiq.cloudbreak.service.cluster.flow.status

import com.sequenceiq.cloudbreak.api.model.Status

enum class ClusterStatus private constructor(val stackStatus: Status, val clusterStatus: Status, val statusReason: String) {
    UNKNOWN(null, null, "Error happened during the communication with Ambari"),
    AMBARISERVER_NOT_RUNNING(null, null, "Ambariserver is not running."),
    AMBARISERVER_RUNNING(Status.AVAILABLE, null, "Ambari server is running."),
    INSTALLING(Status.AVAILABLE, null, "Ambari server is running, services are being installed..."),
    INSTALLED(Status.AVAILABLE, Status.STOPPED, "Services are installed but not running."),
    INSTALL_FAILED(Status.AVAILABLE, null, "Ambari server is running, but the ambari installation has failed."),
    STARTING(Status.AVAILABLE, Status.START_IN_PROGRESS, "Services are installed, starting..."),
    STARTED(Status.AVAILABLE, Status.AVAILABLE, "Services are installed and running."),
    STOPPING(Status.AVAILABLE, Status.STOP_IN_PROGRESS, "Services are installed, stopping..."),
    PENDING(Status.AVAILABLE, null, "There are in progress or pending operations in Ambari. Wait them to be finsihed and try syncing later."),
    AMBIGUOUS(Status.AVAILABLE, null, "There are stopped and running Ambari services as well. Restart or stop all of them and try syncing later.")
}
