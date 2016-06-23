package com.sequenceiq.cloudbreak.service.network

enum class ExposedService {
    SSH("SSH"),
    HTTPS("HTTPS"),
    GATEWAY("Gateway"),
    AMBARI("Ambari"),
    CONSUL("Consul"),
    NAMENODE("NameNode", "NAMENODE"),
    RESOURCEMANAGER_WEB("RM Web", "RESOURCEMANAGER"),
    RESOURCEMANAGER_SCHEDULER("RM Scheduler"),
    RESOURCEMANAGER_IPC("RM IPC"),
    JOB_HISTORY_SERVER("Job History Server", "HISTORYSERVER"),
    HBASE_MASTER("HBase Master"),
    HBASE_MASTER_WEB("HBase Master Web", "HBASE_MASTER"),
    HBASE_REGION("HBase Region Server"),
    HBASE_REGION_INFO("HBase Region Server Info"),
    HIVE_METASTORE("Hive Metastore"),
    HIVE_SERVER("Hive Server"),
    HIVE_SERVER_HTTP("Hive Server Http"),
    FALCON("Falcon", "FALCON_SERVER"),
    STORM("Storm", "STORM"),
    OOZIE("Oozie", "OOZIE_SERVER", "/oozie"),
    ACCUMULO_MASTER("Accumulo Master"),
    ACCUMULO_TSERVER("Accumulo Tserver"),
    ATLAS("Atlas", "ATLAS_SERVER"),
    KNOX_GW("Knox GW"),
    SPARK_HISTORY_SERVER("Spark History server", "SPARK_JOBHISTORYSERVER"),
    CONTAINER_LOGS("Container logs"),
    ZEPPELIN_WEB_SOCKET("Zeppelin web socket"),
    ZEPPELIN_UI("Zeppelin UI", "ZEPPELIN_MASTER"),
    RANGER("Ranger Admin UI", "RANGER_ADMIN"),
    KIBANA("Kibana", "KIBANA"),
    ELASTIC_SEARCH("Elastic Search", "ELASTIC_SEARCH"),
    SWARM("Swarm", "SWARM"),
    SHIPYARD("Shipyard", "SHIPYARD");

    var serviceName: String? = null
        private set
    var portName: String? = null
        private set
    var postFix: String? = null
        private set

    private constructor(portName: String, serviceName: String, postFix: String) {
        this.portName = portName
        this.serviceName = serviceName
        this.postFix = postFix
    }

    private constructor(portName: String, serviceName: String) {
        this.portName = portName
        this.serviceName = serviceName
        this.postFix = ""
    }

    private constructor(portName: String) {
        this.portName = portName
        this.serviceName = portName
        this.postFix = ""
    }
}
