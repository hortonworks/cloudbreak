package com.sequenceiq.cloudbreak.cluster.api;

import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;

public interface ClusterKraftMigrationStatusService {
    KraftMigrationStatus getKraftMigrationStatus();
}
