package com.sequenceiq.cloudbreak.api.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public enum ExposedService {

    ALL("ALL", "ALL", "", "", ""),
    SSH("SSH", "SSH", "", "", ""),
    HTTPS("HTTPS", "HTTPS", "", "", ""),
    GATEWAY("Gateway", "Gateway", "", "", ""),
    AMBARI("Ambari", "AMBARI_SERVER", "", "AMBARI", "/ambari/"),
    AMBARIUI("Ambari", "AMBARI_SERVER", "", "AMBARIUI", "/ambari/"),
    CONSUL("Consul", "Consul", "", "", ""),
    WEBHDFS("WebHDFS", "NAMENODE", "", "WEBHDFS", "/webhdfs/"),
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
    HIVE_SERVER("Hive Server", "HIVE_SERVER", "", "", ""),
    HIVE_SERVER_INTERACTIVE("Hive Server Interactive", "HIVE_SERVER_INTERACTIVE", "", "", ""),
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
    ZEPPELIN_WEB_SOCKET("Zeppelin Web Socket", "ZEPPELIN_MASTER", "", "ZEPPELINWS", ""),
    ZEPPELIN_UI("Zeppelin UI", "ZEPPELIN_MASTER", "", "ZEPPELINUI", "/zeppelin/"),
    RANGER("Ranger Admin UI", "RANGER_ADMIN", "", "RANGERUI", "/ranger/"),
    KIBANA("Kibana", "KIBANA", "", "", ""),
    ELASTIC_SEARCH("Elastic Search", "ELASTIC_SEARCH", "", "", ""),
    DRUID_SUPERSET("Druid Superset", "DRUID_SUPERSET", "", "", "");

    private final String serviceName;
    private final String portName;
    private final String postfix;
    private final String knoxService;
    private final String knoxUrl;

    ExposedService(String portName, String serviceName, String postFix, String knoxService, String knoxUrl) {
        this.portName = portName;
        this.serviceName = serviceName;
        postfix = postFix;
        this.knoxService = knoxService;
        this.knoxUrl = knoxUrl;
    }

    public static List<ExposedService> filterSupportedKnoxServices() {
        return Arrays.stream(values()).filter(x -> {
            return !Strings.isNullOrEmpty(x.knoxService);
        }).collect(Collectors.toList());
    }

    public static List<String> getAllKnoxExposed() {
        List<String> allKnoxExposed = filterSupportedKnoxServices().stream().map(ExposedService::getKnoxService).collect(Collectors.toList());
        return ImmutableList.copyOf(allKnoxExposed);
    }

    public static List<String> getAllServiceName() {
        List<String> allServiceName = Arrays.stream(values()).filter(x -> {
            return !Strings.isNullOrEmpty(x.serviceName);
        })
                .map(ExposedService::getServiceName).collect(Collectors.toList());
        return ImmutableList.copyOf(allServiceName);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getPortName() {
        return portName;
    }

    public String getPostFix() {
        return postfix;
    }

    public String getKnoxService() {
        return knoxService;
    }

    public String getKnoxUrl() {
        return knoxUrl;
    }
}
