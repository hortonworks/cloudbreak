package com.sequenceiq.cloudbreak.api.endpoint.v4;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public enum ExposedService {

    ALL("Every Service", "ALL", "", "", true, null),

    AMBARI("Ambari", "AMBARI_SERVER", "AMBARI", "/ambari/", true, 8080),
    WEBHDFS("WebHDFS", "NAMENODE", "WEBHDFS", "/webhdfs/v1", false, 50070),
    NAMENODE("Name Node", "NAMENODE", "HDFSUI", "/hdfs/", true, 50070),
    RESOURCEMANAGER_WEB("Resource Manager", "RESOURCEMANAGER", "YARNUI", "/yarn/", true, 8088),
    RESOURCEMANAGER_WEB_V2("Resource Manager V2", "RESOURCEMANAGER", "YARNUIV2", "/yarnuiv2/", true, 8088),
    JOB_HISTORY_SERVER("Job History Server", "HISTORYSERVER", "JOBHISTORYUI", "/jobhistory/", true, 19888),
    HIVE_SERVER("Hive Server", "HIVE_SERVER", "HIVE", "/hive/", false, 10001),
    HIVE_SERVER_INTERACTIVE("Hive Server Interactive", "HIVE_SERVER_INTERACTIVE", "HIVE_INTERACTIVE", "/hive/", false, 10501),
    ATLAS("Atlas", "ATLAS_SERVER", "ATLAS", "/atlas/", true, 21000),
    SPARK_HISTORY_SERVER("Spark 1.x History Server", "SPARK_JOBHISTORYSERVER", "SPARKHISTORYUI", "/sparkhistory/", true, 18080),
    SPARK2_HISTORY_SERVER("Spark History Server", "SPARK2_JOBHISTORYSERVER", "SPARK2HISTORYUI", "/sparkhistory/", true, 18081),
    ZEPPELIN("Zeppelin", "ZEPPELIN_MASTER", "ZEPPELIN", "/zeppelin/", false, 9995),
    RANGER("Ranger", "RANGER_ADMIN", "RANGER", "/ranger/", true, 6080),
    DP_PROFILER_AGENT("DP Profiler Agent", "DP_PROFILER_AGENT", "PROFILER-AGENT", "", true, 21900),
    BEACON_SERVER("Beacon", "BEACON_SERVER", "BEACON", "", true, 25968),
    LIVY_SERVER("Livy Server", "LIVY2_SERVER", "LIVYSERVER", "/livy/v1/sessions/", true, 8999),
    LOGSEARCH("Log Search", "LOGSEARCH_SERVER", "LOGSEARCH", "/logsearch", true, 61888);

    private final String serviceName;
    private final String portName;
    private final String knoxService;
    private final String knoxUrl;
    private final boolean ssoSupported;
    private final Integer defaultPort;

    ExposedService(String portName, String serviceName, String knoxService, String knoxUrl, boolean ssoSupported, Integer defaultPort) {
        this.portName = portName;
        this.serviceName = serviceName;
        this.knoxService = knoxService;
        this.knoxUrl = knoxUrl;
        this.ssoSupported = ssoSupported;
        this.defaultPort = defaultPort;
    }

    public static boolean isKnoxExposed(String knoxService) {
        return getAllKnoxExposed().contains(knoxService);
    }

    public static Collection<ExposedService> filterSupportedKnoxServices() {
        return Arrays.stream(values()).filter(x -> !Strings.isNullOrEmpty(x.knoxService)).collect(Collectors.toList());
    }

    public static Collection<ExposedService> knoxServicesForComponents(Collection<String> components) {
        Collection<ExposedService> supportedKnoxServices = filterSupportedKnoxServices();
        return supportedKnoxServices.stream()
                .filter(exposedService ->
                        components.contains(exposedService.serviceName)
                                || "AMBARI_SERVER".equals(exposedService.serviceName))
                .collect(Collectors.toList());
    }

    public static List<String> getAllKnoxExposed() {
        List<String> allKnoxExposed = filterSupportedKnoxServices().stream().map(ExposedService::getKnoxService).collect(Collectors.toList());
        return ImmutableList.copyOf(allKnoxExposed);
    }

    public static List<String> getAllServiceName() {
        List<String> allServiceName = Arrays.stream(values()).filter(x -> !Strings.isNullOrEmpty(x.serviceName))
                .map(ExposedService::getServiceName).collect(Collectors.toList());
        return ImmutableList.copyOf(allServiceName);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getPortName() {
        return portName;
    }

    public String getKnoxService() {
        return knoxService;
    }

    public String getKnoxUrl() {
        return knoxUrl;
    }

    public boolean isSSOSupported() {
        return ssoSupported;
    }

    public Integer getDefaultPort() {
        return defaultPort;
    }
}
