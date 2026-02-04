package com.sequenceiq.common.model;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public enum CloudStorageCdpService {

    ZEPPELIN_NOTEBOOK,
    YARN_LOG,
    HIVE_METASTORE_WAREHOUSE,
    HIVE_METASTORE_EXTERNAL_WAREHOUSE,
    HIVE_REPLICA_WAREHOUSE,
    HBASE_ROOT,
    RANGER_AUDIT,
    PROFILER_SERVICE_FS_URI,
    DEFAULT_FS,
    REMOTE_FS,
    FLINK_HISTORYSERVER_ARCHIVE,
    FLINK_JOBMANAGER_ARCHIVE,
    FLINK_CHECKPOINTS,
    FLINK_SAVEPOINTS,
    FLINK_HIGH_AVAILABILITY,
    ICEBERG_REPLICATION_CLOUD_ROOT,
    NIFI_LOGS_REPLICATION;

    @JsonIgnore
    public static String typeListing() {
        return Arrays.stream(CloudStorageCdpService.values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
