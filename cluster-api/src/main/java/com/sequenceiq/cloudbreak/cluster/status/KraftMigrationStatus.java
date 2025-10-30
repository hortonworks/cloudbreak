package com.sequenceiq.cloudbreak.cluster.status;

public enum KraftMigrationStatus {
    ZOOKEEPER_INSTALLED,
    PRE_MIGRATION,
    BROKERS_IN_MIGRATION,
    BROKERS_IN_KRAFT,
    KRAFT_INSTALLED,
    NOT_APPLICABLE
}
