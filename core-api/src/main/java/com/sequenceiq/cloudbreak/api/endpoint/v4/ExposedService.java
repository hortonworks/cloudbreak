package com.sequenceiq.cloudbreak.api.endpoint.v4;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;

public enum ExposedService {

    ALL("Every Service", "ALL", "", "", true, null, null, false, true),

    CLOUDERA_MANAGER("CM-API", "CM-API", "CM-API", "/cm-api/", true, 7180, 7183, true, true),
    CLOUDERA_MANAGER_UI("CM-UI", "CM-UI", "CM-UI", "/cmf/home/", true, 7180, 7183, false, false),
    HUE("HUE", "HUE_LOAD_BALANCER", "HUE", "/hue/", true, 8889, 8889, false, false),
    WEBHDFS("WebHDFS", "NAMENODE", "WEBHDFS", "/webhdfs/v1/", true, 9870, 9871, true, true),
    NAMENODE("Name Node", "NAMENODE", "HDFSUI", "/hdfs/", true, 9870, 9871, false, false),
    RESOURCEMANAGER_WEB("Resource Manager", "RESOURCEMANAGER", "YARNUIV2", "/yarnuiv2/", true, 8088, 8090, false, true),
    JOB_HISTORY_SERVER("Job History Server", "JOBHISTORY", "JOBHISTORYUI", "/jobhistory/", true, 19888, 19890, false, false),
    HIVE_SERVER("Hive Server", "HIVESERVER2", "HIVE", "/hive/", false, 10001, 10001, true, true),
    HIVE_SERVER_INTERACTIVE("Hive Server Interactive", "", "HIVE_INTERACTIVE", "/hive/", false, 10501, 10502, true, true),
    ATLAS("Atlas", "ATLAS_SERVER", "ATLAS", "/atlas/", true, 21000, 31443, false, true),
    SPARK_HISTORY_SERVER("Spark History Server", "SPARK_YARN_HISTORY_SERVER", "SPARKHISTORYUI", "/sparkhistory/", true, 18088, 18488, false, false),
    ZEPPELIN("Zeppelin", "ZEPPELIN_SERVER", "ZEPPELIN", "/zeppelin/", true, 8885, 8886, false, false),
    RANGER("Ranger", "RANGER_ADMIN", "RANGER", "/ranger/", true, 6080, 6182, false, true),
    LIVY_SERVER("Livy Server", "LIVY_SERVER", "LIVYSERVER1", "/livy/ui/", true, 8998, 8998, false, false),
    LIVY_SERVER_API("Livy Server", "LIVY_SERVER", "LIVYSERVER_API", "/livy/", true, 8998, 8998, true, true),
    OOZIE_UI("Oozie Server", "OOZIE_SERVER", "OOZIE", "/oozie/", true, 11000, 11443, false, true, false),
    SOLR("Solr Server", "SOLR_SERVER", "SOLR", "/solr/", true, 8983, 8985, false, true),
    HBASE_UI("HBase UI", "MASTER", "HBASEUI", "/hbase/webui/master/", true, 16010, 16010, false, false),
    HBASE_REST("HBase Rest", "HBASERESTSERVER", "WEBHBASE", "/hbase/", true, 20550, 20550, true, true),
    NIFI("Nifi", "NIFI_NODE", "NIFI", "/nifi-app/nifi/", true, 8080, 8443, false, false),
    NIFI_REGISTRY("Nifi Registry", "NIFI_REGISTRY_SERVER", "NIFI-REGISTRY", "/nifi-registry-app/nifi-registry/", true, 18080, 18433, false, false),
    NIFI_REST("NiFi Rest", "NIFI_NODE", "NIFI_REST", "/nifi-app/nifi-api/", true, 8080, 8443, true, true),
    SMM_UI("Streams Messaging Manager", "STREAMS_MESSAGING_MANAGER_UI", "SMM-UI", "/smm-ui/", true, 9991, 9991, false, false),
    SMM_API("Streams Messaging Manager Rest", "STREAMS_MESSAGING_MANAGER_SERVER", "SMM-API", "/smm-api/", true, 8585, 8587, true, true),
    SCHEMA_REGISTRY("Schema Registry", "SCHEMA_REGISTRY_SERVER", "SCHEMA-REGISTRY", "/schema-registry/", true, 7788, 7790, false, false),
    IMPALA("Impala", "IMPALAD", "IMPALA", "/impala/", true, 28000, 28000, true, true),
    IMPALA_DEBUG_UI("Impala Debug UI", "IMPALA_DEBUG_UI", "IMPALA_DEBUG_UI", "/impalaui", true, 25000, 25000, false, false),
    NAMENODE_HDFS("NameNode HDFS", "NAMENODE", "NAMENODE", "/", true, 8020, 8020, true, true, false),
    JOBTRACKER("Job Tracker", "RESOURCEMANAGER", "JOBTRACKER", "/", true, 8032, 8032, true, true, false),
    PROFILER_ADMIN("Profiler Admin", "PROFILER_ADMIN_AGENT", "PROFILER-ADMIN-API", "/profiler-admin/", false, 21700, 21700, true, true),
    PROFILER_METRICS("Profiler Metrics", "PROFILER_METRICS_AGENT", "PROFILER-METRICS-API", "/profiler-metrics/", false, 21800, 21800, true, true),
    PROFILER_SCHEDULER("Profiler Scheduler", "PROFILER_SCHEDULER_AGENT", "PROFILER-SCHEDULER-API", "/profiler-scheduler/", false, 21900, 21900, true, true),
    DAS("Data Analytics Studio", "DAS", "DAS", "/das/", true, 30800, 30800, false, true);

    private final String displayName;
    private final String serviceName;
    private final String knoxService;
    private final String knoxUrl;
    private final boolean ssoSupported;
    private final Integer port;
    private final Integer tlsPort;
    private final boolean apiOnly;
    private final boolean apiIncluded;
    private final boolean visible;

    ExposedService(String displayName, String serviceName, String knoxService, String knoxUrl,
            boolean ssoSupported, Integer port, Integer tlsPort, boolean apiOnly, boolean apiIncluded) {
        this(displayName, serviceName, knoxService, knoxUrl, ssoSupported, port, tlsPort, apiOnly, apiIncluded, true);
    }

    ExposedService(String displayName, String serviceName, String knoxService, String knoxUrl,
            boolean ssoSupported, Integer port, Integer tlsPort, boolean apiOnly, boolean apiIncluded, boolean visible) {
        this.displayName = displayName;
        this.serviceName = serviceName;
        this.knoxService = knoxService;
        this.knoxUrl = knoxUrl;
        this.ssoSupported = ssoSupported;
        this.port = port;
        this.tlsPort = tlsPort;
        this.apiOnly = apiOnly;
        this.apiIncluded = apiIncluded;
        this.visible = visible;
    }

    public static boolean isKnoxExposed(String knoxService) {
        return getAllKnoxExposed().contains(knoxService);
    }

    public static Collection<ExposedService> filterSupportedKnoxServices() {
        return Arrays.stream(values()).filter(x -> StringUtils.isNotEmpty(x.knoxService)).collect(Collectors.toList());
    }

    public static Collection<ExposedService> knoxServicesForComponents(Collection<String> components) {
        Collection<ExposedService> supportedKnoxServices = filterSupportedKnoxServices();
        return supportedKnoxServices.stream()
                .filter(exposedService ->
                        components.contains(exposedService.serviceName)
                                || CLOUDERA_MANAGER_UI.serviceName.equals(exposedService.serviceName)
                                || CLOUDERA_MANAGER.serviceName.equals(exposedService.serviceName)
                                // IMPALA_DEBUG_UI needs to be exposed under the same service name, but with different purpose
                                || (IMPALA_DEBUG_UI.serviceName.equals(exposedService.serviceName) && components.contains(IMPALA.serviceName)))
                .collect(Collectors.toList());
    }

    public static List<String> getAllKnoxExposed() {
        List<String> allKnoxExposed = filterSupportedKnoxServices().stream().map(ExposedService::getKnoxService).collect(Collectors.toList());
        return ImmutableList.copyOf(allKnoxExposed);
    }

    public static List<String> getAllServiceName() {
        List<String> allServiceName = Arrays.stream(values()).filter(x -> StringUtils.isNotEmpty(x.serviceName))
                .map(ExposedService::getServiceName).collect(Collectors.toList());
        return ImmutableList.copyOf(allServiceName);
    }

    public static Map<String, Integer> getAllServicePorts(boolean tls) {
        return Arrays.stream(values()).filter(x -> StringUtils.isNotEmpty(x.serviceName))
                .filter(x -> StringUtils.isNotEmpty(x.knoxService))
                .filter(x -> Objects.nonNull(tls ? x.tlsPort : x.port))
                .collect(Collectors.toMap(k -> k.knoxService, v -> tls ? v.tlsPort : v.port));
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

    public Integer getPort() {
        return port;
    }

    public Integer getTlsPort() {
        return tlsPort;
    }

    public boolean isUISupported() {
        return !apiOnly;
    }

    public boolean isAPISupported() {
        return apiIncluded;
    }

    public boolean isVisible() {
        return visible;
    }
}
