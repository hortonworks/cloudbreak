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

    ALL("Every Service", "ALL", "ALL", "", "", true, null, false, true),

    CLOUDERA_MANAGER("CM-API", "CM-API", "CM-API", "CM-API", "/cm-api/", true, null, 7180, true, true),
    CLOUDERA_MANAGER_UI("CM-UI", "CM-UI", "CM-UI", "CM-UI", "/cmf/home", true, null, 7180, false, false),
    AMBARI("Ambari", "AMBARI_SERVER", "", "AMBARI", "/ambari/", true, 8080, null, false, true),
    HUE("HUE", "", "HUE_LOAD_BALANCER", "HUE", "/hue/", true, null, 8889, false, false),
    WEBHDFS("WebHDFS", "NAMENODE", "NAMENODE", "WEBHDFS", "/webhdfs/v1", true, 50070, 9870, true, true),
    NAMENODE("Name Node", "NAMENODE", "NAMENODE", "HDFSUI", "/hdfs/", true, 50070, 9870, false, false),
    RESOURCEMANAGER_WEB("Resource Manager", "RESOURCEMANAGER", "RESOURCEMANAGER", "YARNUI", "/yarn/", true, 8088, false, true),
    RESOURCEMANAGER_WEB_V2("Resource Manager V2", "RESOURCEMANAGER", "RESOURCEMANAGER", "YARNUIV2", "/yarnuiv2/", true, 8088, false, false),
    JOB_HISTORY_SERVER("Job History Server", "HISTORYSERVER", "JOBHISTORY", "JOBHISTORYUI", "/jobhistory/", true, 19888, false, false),
    HIVE_SERVER("Hive Server", "HIVE_SERVER", "HIVESERVER2", "HIVE", "/hive/", false, 10001, true, true),
    HIVE_SERVER_INTERACTIVE("Hive Server Interactive", "HIVE_SERVER_INTERACTIVE", "", "HIVE_INTERACTIVE", "/hive/", false, 10501, true, true),
    ATLAS("Atlas", "ATLAS_SERVER", "ATLAS_SERVER", "ATLAS", "/atlas/", true, 21000, false, true),
    SPARK_HISTORY_SERVER("Spark 1.x History Server", "SPARK_JOBHISTORYSERVER", "SPARK_YARN_HISTORY_SERVER", "SPARKHISTORYUI",
            "/sparkhistory/", true, 18080, 18088, false, false),
    SPARK2_HISTORY_SERVER("Spark History Server", "SPARK2_JOBHISTORYSERVER", "", "SPARK2HISTORYUI",
            "/sparkhistory/", true, 18081, false, false),
    ZEPPELIN("Zeppelin", "ZEPPELIN_MASTER", "ZEPPELIN_SERVER", "ZEPPELIN", "/zeppelin/", false, 9995, 8885, false, false),
    RANGER("Ranger", "RANGER_ADMIN", "RANGER_ADMIN", "RANGER", "/ranger/", true, 6080, 6080, false, true),
    DP_PROFILER_AGENT("DP Profiler Agent", "DP_PROFILER_AGENT", "", "PROFILER-AGENT", "", true, 21900, null, false, false),
    BEACON_SERVER("Beacon", "BEACON_SERVER", "", "BEACON", "/beacon", true, 25968, null, false, false),
    LOGSEARCH("Log Search", "LOGSEARCH_SERVER", "", "LOGSEARCH", "/logsearch", true, 61888, null, false, false),
    LIVY2_SERVER("Livy Server 2", "LIVY2_SERVER", "", "LIVYSERVER", "/livy/v1/sessions/", true, 8999, null, false, false),
    LIVY_SERVER("Livy Server", "", "LIVY_SERVER", "LIVYSERVER1", "/livy/ui", true, null, 8998, false, true),
    OOZIE_UI("Oozie Server", "", "OOZIE_SERVER", "OOZIE", "/oozie/", true, null, 11000, false, true),
    SOLR("Solr Server", "", "SOLR_SERVER", "SOLR", "/solr/", true, null, 8983, false, true),
    HBASE_UI("HBase UI", "", "MASTER", "HBASEUI", "/hbase/webui/master", true, null, 16010, false, false),
    HBASE_REST("HBase Rest", "", "HBASERESTSERVER", "WEBHBASE", "/hbase/", true, null, 20550, true, true);

    private final String ambariServiceName;
    private final String cmServiceName;
    private final String displayName;
    private final String knoxService;
    private final String knoxUrl;
    private final boolean ssoSupported;
    private final Integer ambariPort;
    private final Integer cmPort;
    private final boolean apiOnly;
    private final boolean apiIncluded;

    ExposedService(String displayName, String ambariServiceName, String cmServiceName,
            String knoxService, String knoxUrl, boolean ssoSupported, Integer defaultPort, boolean apiOnly, boolean apiIncluded) {
        this.displayName = displayName;
        this.ambariServiceName = ambariServiceName;
        this.cmServiceName = cmServiceName;
        this.knoxService = knoxService;
        this.knoxUrl = knoxUrl;
        this.ssoSupported = ssoSupported;
        this.ambariPort = defaultPort;
        this.cmPort = defaultPort;
        this.apiOnly = apiOnly;
        this.apiIncluded = apiIncluded;
    }

    ExposedService(String displayName, String ambariServiceName, String cmServiceName,
            String knoxService, String knoxUrl, boolean ssoSupported, Integer ambariPort, Integer cmPort, boolean apiOnly, boolean apiIncluded) {
        this.displayName = displayName;
        this.ambariServiceName = ambariServiceName;
        this.cmServiceName = cmServiceName;
        this.knoxService = knoxService;
        this.knoxUrl = knoxUrl;
        this.ssoSupported = ssoSupported;
        this.ambariPort = ambariPort;
        this.cmPort = cmPort;
        this.apiOnly = apiOnly;
        this.apiIncluded = apiIncluded;
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
                        components.contains(exposedService.cmServiceName)
                                || CLOUDERA_MANAGER_UI.cmServiceName.equals(exposedService.cmServiceName)
                                || CLOUDERA_MANAGER.cmServiceName.equals(exposedService.cmServiceName))
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

    public boolean isUISupported() {
        return !apiOnly;
    }

    public boolean isAPISupported() {
        return apiIncluded;
    }
}
