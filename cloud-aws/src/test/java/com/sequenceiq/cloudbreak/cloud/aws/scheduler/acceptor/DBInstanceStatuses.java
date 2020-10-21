package com.sequenceiq.cloudbreak.cloud.aws.scheduler.acceptor;

import java.util.List;

public final class DBInstanceStatuses {

    /**
     * Taken from <a href="https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Overview.DBInstance.Status.html">DB instance status</a>
     */
    static final List<String> DB_STATUSES = List.of(
            "available",
            "backing-up",
            "backtracking",
            "configuring-enhanced-monitoring",
            "configuring-iam-database-auth",
            "configuring-log-exports",
            "converting-to-vpc",
            "creating",
            "deleting",
            "failed",
            "inaccessible-encryption-credentials",
            "incompatible-network",
            "incompatible-option-group",
            "incompatible-parameters",
            "incompatible-restore",
            "maintenance",
            "modifying",
            "moving-to-vpc",
            "rebooting",
            "renaming",
            "resetting-master-credentials",
            "restore-error",
            "starting",
            "stopped",
            "stopping",
            "storage-full",
            "storage-optimization",
            "upgrading"
    );

    private DBInstanceStatuses() {
    }
}
