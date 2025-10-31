package com.sequenceiq.cloudbreak.api.endpoint.v4.database.base;

public enum DatabaseType {
    HIVE,
    @Deprecated
    CDL_HIVE,
    RANGER,
    @Deprecated
    DRUID,
    @Deprecated
    SUPERSET,
    OOZIE,
    @Deprecated
    AMBARI,
    CLOUDERA_MANAGER,
    @Deprecated
    CLOUDERA_MANAGER_MANAGEMENT_SERVICE_REPORTS_MANAGER,
    @Deprecated
    BEACON,
    REGISTRY,
    HIVE_DAS,
    HUE,
    QUERY_PROCESSOR,
    STREAMS_MESSAGING_MANAGER,
    PROFILER_AGENT,
    PROFILER_METRIC,
    NIFIREGISTRY,
    DATAVIZ,
    EFM,
    KNOX_GATEWAY,
    SQL_STREAM_BUILDER_ADMIN,
    SQL_STREAM_BUILDER_SNAPPER
}
