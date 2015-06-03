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
    FALCON("Falcon", "FALCON_SERVER"),
    STORM("Storm", "STORM"),
    OOZIE("Oozie", "OOZIE_SERVER", "/oozie"),
    SPARK_HISTORY_SERVER("Spark History server", "SPARK_JOBHISTORYSERVER"),
    CONTAINER_LOGS("Container logs"),
    ZEPPELI_WEB_SOCKET("Zeppelin web socket"),
    ZEPPELI_UI("Zeppelin ui", "ZEPPELIN_MASTER"),
    KIBANA("Kibana", "KIBANA"),
    ELASTIC_SEARCH("Elastic Search", "ELASTIC_SEARCH");

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
