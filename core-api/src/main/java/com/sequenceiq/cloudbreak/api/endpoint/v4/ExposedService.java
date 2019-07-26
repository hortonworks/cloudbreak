package com.sequenceiq.cloudbreak.api.endpoint.v4;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

public enum ExposedService {

    ALL("Every Service", "ALL", "", "", true, null, false, true),

    CLOUDERA_MANAGER("CM-API", "CM-API", "CM-API", "/cm-api/", true, 7180, true, true),
    CLOUDERA_MANAGER_UI("CM-UI", "CM-UI", "CM-UI", "/cmf/home", true, 7180, false, false),
    HUE("HUE", "HUE_LOAD_BALANCER", "HUE", "/hue/", true, 8889, false, false),
    WEBHDFS("WebHDFS", "NAMENODE", "WEBHDFS", "/webhdfs/v1", true, 9870, true, true),
    NAMENODE("Name Node", "NAMENODE", "HDFSUI", "/hdfs/", true, 9870, false, false),
    RESOURCEMANAGER_WEB("Resource Manager", "RESOURCEMANAGER", "YARNUIV2", "/yarnuiv2/", true, 8088, false, true),
    JOB_HISTORY_SERVER("Job History Server", "JOBHISTORY", "JOBHISTORYUI", "/jobhistory/", true, 19888, false, false),
    HIVE_SERVER("Hive Server", "HIVESERVER2", "HIVE", "/hive/", false, 10001, true, true),
    HIVE_SERVER_INTERACTIVE("Hive Server Interactive", "", "HIVE_INTERACTIVE", "/hive/", false, 10501, true, true),
    ATLAS("Atlas", "ATLAS_SERVER", "ATLAS", "/atlas/", true, 21000, false, true),
    SPARK_HISTORY_SERVER("Spark History Server", "SPARK_YARN_HISTORY_SERVER", "SPARKHISTORYUI", "/sparkhistory/", true, 18088, false, false),
    ZEPPELIN("Zeppelin", "ZEPPELIN_SERVER", "ZEPPELIN", "/zeppelin/", false, 8885, false, false),
    RANGER("Ranger", "RANGER_ADMIN", "RANGER", "/ranger/", true, 6080, false, true),
    LIVY_SERVER("Livy Server", "LIVY_SERVER", "LIVYSERVER1", "/livy/ui", true, 8998, false, true),
    OOZIE_UI("Oozie Server", "OOZIE_SERVER", "OOZIE", "/oozie/", true, 11000, false, true),
    SOLR("Solr Server", "SOLR_SERVER", "SOLR", "/solr/", true, 8983, false, true),
    HBASE_UI("HBase UI", "MASTER", "HBASEUI", "/hbase/webui/master", true, 16010, false, false),
    HBASE_REST("HBase Rest", "HBASERESTSERVER", "WEBHBASE", "/hbase/", true, 20550, true, true);

    private final String displayName;
    private final String serviceName;
    private final String knoxService;
    private final String knoxUrl;
    private final boolean ssoSupported;
    private final Integer cmPort;
    private final boolean apiOnly;
    private final boolean apiIncluded;

    ExposedService(String displayName, String serviceName, String knoxService, String knoxUrl,
            boolean ssoSupported, Integer cmPort, boolean apiOnly, boolean apiIncluded) {
        this.displayName = displayName;
        this.serviceName = serviceName;
        this.knoxService = knoxService;
        this.knoxUrl = knoxUrl;
        this.ssoSupported = ssoSupported;
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
                        components.contains(exposedService.serviceName)
                                || CLOUDERA_MANAGER_UI.serviceName.equals(exposedService.serviceName)
                                || CLOUDERA_MANAGER.serviceName.equals(exposedService.serviceName))
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

    public static Map<String, Integer> getAllServicePorts() {
        return Arrays.stream(values()).filter(x -> !Strings.isNullOrEmpty(x.serviceName))
                .filter(x -> !Strings.isNullOrEmpty(x.knoxService))
                .filter(x -> Objects.nonNull(x.cmPort))
                .collect(Collectors.toMap(k -> k.knoxService, v -> v.cmPort));
    }

    public String getServiceName() {
        return serviceName;
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
