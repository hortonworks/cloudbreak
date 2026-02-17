package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate;

public enum ClusterTemplateV4Type {
    SPARK,
    HIVE,
    EDW,
    ETL,
    DATASCIENCE,
    DATAMART,
    ICEBERG,
    DATALAKE,
    DATAENGINEERING,
    DATAENGINEERING_HA,
    STREAMING,
    STREAMING_HA,
    FLOW_MANAGEMENT,
    FLOW_MANAGEMENT_HA,
    OPERATIONALDATABASE,
    DISCOVERY_DATA_AND_EXPLORATION,
    OTHER,
    DLM,
    LAKEHOUSE_OPTIMIZER,
    HYBRID_DATAENGINEERING_HA,
    HYBRID_STREAMS_MESSAGING;

    public boolean isHybrid() {
        return name().startsWith("HYBRID_");
    }
}
