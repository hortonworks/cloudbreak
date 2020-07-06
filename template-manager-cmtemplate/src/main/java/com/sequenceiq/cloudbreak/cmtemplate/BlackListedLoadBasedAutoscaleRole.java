package com.sequenceiq.cloudbreak.cmtemplate;

public enum BlackListedLoadBasedAutoscaleRole {
    DATANODE,
    ZEPPELIN_SERVER,
    KAFKA_BROKER,
    SCHEMA_REGISTRY_SERVER,
    STREAMS_MESSAGING_MANAGER_SERVER,
    // The following item means Zookeeper server.
    SERVER,
    NIFI_NODE,
    NAMENODE,
    IMPALAD,
    STATESTORE,
    CATALOGSERVER,
    KUDU_MASTER,
    KUDU_TSERVER,
    SOLR_SERVER,
    NIFI_REGISTRY_SERVER,
    HUE_LOAD_BALANCER,
    KNOX_GATEWAY
}
