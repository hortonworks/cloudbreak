package com.sequenceiq.cloudbreak.api.endpoint.v4;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public enum ExposedService {

    ALL("Every Service", "ALL", "ALL", "", "", true, null),

    CLOUDERA_MANAGER("CM", "CM-API", "CM-API", "CM-API", "/cm-api/", true, null, 7180),
    AMBARI("Ambari", "AMBARI_SERVER", "", "AMBARI", "/ambari/", true, 8080, null),
    WEBHDFS("WebHDFS", "NAMENODE", "NAMENODE", "WEBHDFS", "/webhdfs/v1", false, 50070),
    NAMENODE("Name Node", "NAMENODE", "NAMENODE", "HDFSUI", "/hdfs/", true, 50070, 9870),
    RESOURCEMANAGER_WEB("Resource Manager", "RESOURCEMANAGER", "RESOURCEMANAGER", "YARNUI", "/yarn/", true, 8088),
    RESOURCEMANAGER_WEB_V2("Resource Manager V2", "RESOURCEMANAGER", "RESOURCEMANAGER", "YARNUIV2", "/yarnuiv2/", true, 8088),
    JOB_HISTORY_SERVER("Job History Server", "HISTORYSERVER", "JOBHISTORY", "JOBHISTORYUI", "/jobhistory/", true, 19888),
    HIVE_SERVER("Hive Server", "HIVE_SERVER", "HIVESERVER2", "HIVE", "/hive/", false, 10001, 10002),
    HIVE_SERVER_INTERACTIVE("Hive Server Interactive", "HIVE_SERVER_INTERACTIVE", "", "HIVE_INTERACTIVE", "/hive/", false, 10501),
    ATLAS("Atlas", "ATLAS_SERVER", "ATLAS_SERVER", "ATLAS", "/atlas/", true, 21000),
    SPARK_HISTORY_SERVER("Spark 1.x History Server", "SPARK_JOBHISTORYSERVER", "SPARK_YARN_HISTORY_SERVER", "SPARKHISTORYUI",
            "/sparkhistory/", true, 18080, 18088),
    SPARK2_HISTORY_SERVER("Spark History Server", "SPARK2_JOBHISTORYSERVER", "", "SPARK2HISTORYUI",
            "/sparkhistory/", true, 18081),
    ZEPPELIN("Zeppelin", "ZEPPELIN_MASTER", "ZEPPELIN_SERVER", "ZEPPELIN", "/zeppelin/", false, 9995, 8885),
    RANGER("Ranger", "RANGER_ADMIN", "RANGER_ADMIN", "RANGER", "/ranger/", true, 6080, 6080),
    DP_PROFILER_AGENT("DP Profiler Agent", "DP_PROFILER_AGENT", "", "PROFILER-AGENT", "", true, 21900, null),
    BEACON_SERVER("Beacon", "BEACON_SERVER", "", "BEACON", "/beacon", true, 25968, null),
    LOGSEARCH("Log Search", "LOGSEARCH_SERVER", "", "LOGSEARCH", "/logsearch", true, 61888, null),
    LIVY2_SERVER("Livy Server 2", "LIVY2_SERVER", "", "LIVYSERVER", "/livy/v1/sessions/", true, 8999, null),
    LIVY_SERVER("Livy Server", "", "LIVY_SERVER", "LIVYSERVER1", "/livy/v1/", true, null, 8998),
    OOZIE_UI("Oozie Server", "", "OOZIE_SERVER", "OOZIE", "/oozie/", true, null, 11000);

    private final String ambariServiceName;
    private final String cmServiceName;
    private final String displayName;
    private final String knoxService;
    private final String knoxUrl;
    private final boolean ssoSupported;
    private final Integer ambariPort;
    private final Integer cmPort;

    ExposedService(String displayName, String ambariServiceName, String cmServiceName,
            String knoxService, String knoxUrl, boolean ssoSupported, Integer defaultPort) {
        this.displayName = displayName;
        this.ambariServiceName = ambariServiceName;
        this.cmServiceName = cmServiceName;
        this.knoxService = knoxService;
        this.knoxUrl = knoxUrl;
        this.ssoSupported = ssoSupported;
        this.ambariPort = defaultPort;
        this.cmPort = defaultPort;
    }

    ExposedService(String displayName, String ambariServiceName, String cmServiceName,
            String knoxService, String knoxUrl, boolean ssoSupported, Integer ambariPort, Integer cmPort) {
        this.displayName = displayName;
        this.ambariServiceName = ambariServiceName;
        this.cmServiceName = cmServiceName;
        this.knoxService = knoxService;
        this.knoxUrl = knoxUrl;
        this.ssoSupported = ssoSupported;
        this.ambariPort = ambariPort;
        this.cmPort = cmPort;
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
                        components.contains(exposedService.ambariServiceName)
                                || "AMBARI_SERVER".equals(exposedService.ambariServiceName))
                .collect(Collectors.toList());
    }

    public static Collection<ExposedService> knoxServicesForCmComponents(Collection<String> components) {
        Collection<ExposedService> supportedKnoxServices = filterSupportedKnoxServices();
        return supportedKnoxServices.stream()
                .filter(exposedService ->
                        components.contains(exposedService.cmServiceName))
                .collect(Collectors.toList());
    }

    public static List<String> getAllKnoxExposed() {
        List<String> allKnoxExposed = filterSupportedKnoxServices().stream().map(ExposedService::getKnoxService).collect(Collectors.toList());
        return ImmutableList.copyOf(allKnoxExposed);
    }

    public static List<String> getAllServiceNameForAmbari() {
        List<String> allServiceName = Arrays.stream(values()).filter(x -> !Strings.isNullOrEmpty(x.ambariServiceName))
                .map(ExposedService::getAmbariServiceName).collect(Collectors.toList());
        return ImmutableList.copyOf(allServiceName);
    }

    public static List<String> getAllServiceNameForCM() {
        List<String> allServiceName = Arrays.stream(values()).filter(x -> !Strings.isNullOrEmpty(x.cmServiceName))
                .map(ExposedService::getCmServiceName).collect(Collectors.toList());
        return ImmutableList.copyOf(allServiceName);
    }

    public static Map<String, Integer> getAllServicePortsForCM() {
        return Arrays.stream(values()).filter(x -> !Strings.isNullOrEmpty(x.cmServiceName))
                .filter(x -> !Strings.isNullOrEmpty(x.knoxService))
                .filter(x -> Objects.nonNull(x.cmPort))
                .collect(Collectors.toMap(k -> k.knoxService, v -> v.cmPort));
    }

    public String getAmbariServiceName() {
        return ambariServiceName;
    }

    public static String getServiceNameBasedOnClusterVariant(ExposedService exposedService) {
        return StringUtils.isEmpty(exposedService.ambariServiceName) ? exposedService.cmServiceName : exposedService.ambariServiceName;
    }

    public String getCmServiceName() {
        return cmServiceName;
    }

    public String getDisplayName() {
        return displayName;
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

    public Integer getAmbariPort() {
        return ambariPort;
    }

    public Integer getCmPort() {
        return cmPort;
    }

}
