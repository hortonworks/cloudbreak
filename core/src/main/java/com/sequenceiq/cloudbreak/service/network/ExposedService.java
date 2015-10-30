package com.sequenceiq.cloudbreak.service.network;

public enum ExposedService {
    AMBARI("Ambari"),
    NAMENODE("NameNode", "NAMENODE"),
    RESOURCEMANAGER_WEB("RM Web", "RESOURCEMANAGER"),
    JOB_HISTORY_SERVER("Job History Server", "HISTORYSERVER"),
    HBASE_MASTER_WEB("HBase Master Web", "HBASE_MASTER"),
    FALCON("Falcon", "FALCON_SERVER"),
    STORM("Storm", "STORM"),
    OOZIE("Oozie", "OOZIE_SERVER", "/oozie"),
    ACCUMULO_MASTER("Accumulo Master", "ACCUMULO_MASTER"),
    ACCUMULO_TSERVER("Accumulo Tserver", "ACCUMULO_TSERVER"),
    ATLAS("Atlas", "ATLAS_SERVER"),
    SPARK_HISTORY_SERVER("Spark History server", "SPARK_JOBHISTORYSERVER"),
    CONTAINER_LOGS("Container logs"),
    ZEPPELIN_UI("Zeppelin ui", "ZEPPELIN_MASTER"),
    KIBANA("Kibana", "KIBANA"),
    ELASTIC_SEARCH("Elastic Search", "ELASTIC_SEARCH"),
    RANGER("Ranger", "RANGER");

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
