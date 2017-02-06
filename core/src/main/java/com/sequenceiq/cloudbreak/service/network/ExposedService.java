package com.sequenceiq.cloudbreak.service.network;

public enum ExposedService {
    SSH("SSH", "SSH", "", "", ""),
    HTTPS("HTTPS", "HTTPS", "", "", ""),
    GATEWAY("Gateway", "Gateway", "", "", ""),
    AMBARI("Ambari", "Ambari", "", "AMBARIUI", "/ambari/"),
    CONSUL("Consul", "Consul", "", "", ""),
    NAMENODE("Name Node", "NAMENODE", "", "HDFSUI", "/hdfs/"),
    RESOURCEMANAGER_WEB("Resource Manager", "RESOURCEMANAGER", "", "YARNUI", "/yarn/"),
    RESOURCEMANAGER_SCHEDULER("RM Scheduler", "RM Scheduler", "", "", ""),
    RESOURCEMANAGER_IPC("RM IPC", "RM IPC", "", "", ""),
    JOB_HISTORY_SERVER("Job History Server", "HISTORYSERVER", "", "JOBHISTORYUI", "/jobhistory/"),
    HBASE_MASTER("HBase Master", "HBase Master", "", "", ""),
    HBASE_MASTER_WEB("HBase Master Web", "HBASE_MASTER", "", "", ""),
    HBASE_REGION("HBase Region Server", "HBase Region Server", "", "", ""),
    HBASE_REGION_INFO("HBase Region Server Info", "HBase Region Server Info", "", "", ""),
    HIVE_METASTORE("Hive Metastore", "Hive Metastore", "", "", ""),
    HIVE_SERVER("Hive Server", "ive Server", "", "", ""),
    HIVE_SERVER_HTTP("Hive Server Http", "Hive Server Http", "", "", ""),
    FALCON("Falcon", "FALCON_SERVER", "", "", ""),
    STORM("Storm", "STORM", "", "", ""),
    OOZIE("Oozie", "OOZIE_SERVER", "/oozie", "", ""),
    ACCUMULO_MASTER("Accumulo Master", "Accumulo Master", "", "", ""),
    ACCUMULO_TSERVER("Accumulo Tserver", "Accumulo Tserver", "", "", ""),
    ATLAS("Atlas", "ATLAS_SERVER", "", "", ""),
    KNOX_GW("Knox GW", "Knox GW", "", "", ""),
    SPARK_HISTORY_SERVER("Spark History Server", "SPARK_JOBHISTORYSERVER", "", "SPARKHISTORYUI", "/sparkhistory/"),
    CONTAINER_LOGS("Container logs", "Container logs", "", "", ""),
    ZEPPELIN_WEB_SOCKET("Zeppelin web socket", "Zeppelin web socket", "", "", ""),
    ZEPPELIN_UI("Zeppelin UI", "ZEPPELIN_MASTER", "", "ZEPPELINUI", "/zeppelin/"),
    RANGER("Ranger Admin UI", "RANGER_ADMIN", "", "RANGERUI", "/ranger/"),
    KIBANA("Kibana", "KIBANA", "", "", ""),
    ELASTIC_SEARCH("Elastic Search", "ELASTIC_SEARCH", "", "", ""),
    SWARM("Swarm", "SWARM", "", "", ""),
    SHIPYARD("Shipyard", "SHIPYARD", "", "", "");

    private final String serviceName;
    private final String portName;
    private final String postfix;
    private final String knoxService;
    private final String knoxUrl;

    ExposedService(String portName, String serviceName, String postFix, String knoxService, String knoxUrl) {
        this.portName = portName;
        this.serviceName = serviceName;
        this.postfix = postFix;
        this.knoxService = knoxService;
        this.knoxUrl = knoxUrl;
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

    public String getKnoxService() {
        return knoxService;
    }

    public String getKnoxUrl() {
        return knoxUrl;
    }
}
