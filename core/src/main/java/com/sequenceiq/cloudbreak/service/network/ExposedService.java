package com.sequenceiq.cloudbreak.service.network;

public enum ExposedService {
    SSH("SSH"),
    GATEWAY("Gateway"),
    AMBARI("Ambari"),
    CONSUL("Consul"),
    NAMENODE("NameNode", "NAMENODE"),
    RESOURCEMANAGER_WEB("RM Web", "RESOURCEMANAGER"),
    RESOURCEMANAGER_SCHEDULER("RM Scheduler"),
    RESOURCEMANAGER_IPC("RM IPC"),
    JOB_HISTORY_SERVER("Job History Server", "HISTORYSERVER"),
    HBASE_MASTER("HBase Master", "HBASE_MASTER"),
    HBASE_MASTER_WEB("HBase Master Web", "HBASE_MASTER"),
    HBASE_REGION("HBase Region Server", "HBASE_REGIONSERVER"),
    HBASE_REGION_INFO("HBase Region Server Info", "HBASE_REGIONSERVER"),
    HIVE_METASTORE("Hive Metastore", "HIVE_METASTORE"),
    HIVE_SERVER("Hive Server", "HIVE_SERVER"),
    HIVE_SERVER_HTTP("Hive Server Http", "HIVE_SERVER"),
    FALCON("Falcon", "FALCON_SERVER"),
    STORM("Storm", "STORM"),
    OOZIE("Oozie", "OOZIE_SERVER", "/oozie"),
    ACCUMULO_MASTER("Accumulo Master", "ACCUMULO_MASTER"),
    ACCUMULO_TSERVER("Accumulo Tserver", "ACCUMULO_TSERVER"),
    ATLAS("Atlas", "ATLAS_SERVER"),
    KNOX_GW("Knox GW", "KNOX_GATEWAY"),
    SPARK_HISTORY_SERVER("Spark History server", "SPARK_JOBHISTORYSERVER"),
    CONTAINER_LOGS("Container logs"),
    ZEPPELIN_WEB_SOCKET("Zeppelin web socket"),
    ZEPPELIN_UI("Zeppelin UI", "ZEPPELIN_MASTER"),
    RANGER("Ranger Admin UI", "RANGER_ADMIN"),
    KIBANA("Kibana", "KIBANA"),
    ELASTIC_SEARCH("Elastic Search", "ELASTIC_SEARCH"),
    SWARM("Swarm", "SWARM");

    private String serviceName;
    private String portName;
    private String postfix;

    private ExposedService(String portName, String serviceName, String postFix) {
        this.portName = portName;
        this.serviceName = serviceName;
        this.postfix = postFix;
    }

    private ExposedService(String portName, String serviceName) {
        this.portName = portName;
        this.serviceName = serviceName;
        this.postfix = "";
    }

    private ExposedService(String portName) {
        this.portName = portName;
        this.serviceName = portName;
        this.postfix = "";
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public String getPortName() {
        return this.portName;
    }

    public String getPostFix() {
        return this.postfix;
    }
}
