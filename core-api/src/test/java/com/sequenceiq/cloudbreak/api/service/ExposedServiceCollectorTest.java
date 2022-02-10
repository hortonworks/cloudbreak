package com.sequenceiq.cloudbreak.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@ExtendWith(MockitoExtension.class)
class ExposedServiceCollectorTest {

    private static final Optional<String> CDH_7_2_10 = Optional.of("7.2.10");

    private static final Optional<String> CDH_7_2_12 = Optional.of("7.2.12");

    private static final Optional<String> CDH_7_2_14 = Optional.of("7.2.14");

    @InjectMocks
    private ExposedServiceCollector underTest;

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Spy
    private ExposedServiceVersionSupport exposedServiceVersionSupportService;

    @BeforeEach
    void setUp() throws IOException {
        String rawJson = getTestExposedServices();
        when(cloudbreakResourceReaderService.resourceDefinition(anyString())).thenReturn(rawJson);
    }

    private String getTestExposedServices() throws IOException {
        String rawJson;
        try (BufferedReader bs = new BufferedReader(
                new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("definitions/exposed-services.json")))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = bs.readLine()) != null) {
                out.append(line);
            }
            rawJson = out.toString();
        }
        return rawJson;
    }

    @Test
    void initIllegal() {
        when(cloudbreakResourceReaderService.resourceDefinition(anyString())).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> underTest.init());
    }

    @Test
    void hasAllServices() {
        underTest.init();
        assertEquals("ATLAS", underTest.getAtlasService().getName());
        assertEquals("CLOUDERA_MANAGER", underTest.getClouderaManagerService().getName());
        assertEquals("CLOUDERA_MANAGER_UI", underTest.getClouderaManagerUIService().getName());
        assertEquals("HBASE_REST", underTest.getHBaseRestService().getName());
        assertEquals("HBASE_UI", underTest.getHBaseUIService().getName());
        assertEquals("HBASEJARS", underTest.getHBaseJarsService().getName());
        assertEquals("HIVE_SERVER", underTest.getHiveServerService().getName());
        assertEquals("HUE", underTest.getHueService().getName());
        assertEquals("IMPALA", underTest.getImpalaService().getName());
        assertEquals("IMPALA_DEBUG_UI", underTest.getImpalaDebugUIService().getName());
        assertEquals("KUDU", underTest.getKuduService().getName());
        assertEquals("NAMENODE", underTest.getNameNodeService().getName());
        assertEquals("NIFI", underTest.getNiFiService().getName());
        assertEquals("RANGER", underTest.getRangerService().getName());
        assertEquals("RESOURCEMANAGER_WEB", underTest.getResourceManagerWebService().getName());
    }

    @Test
    void getAllServicesNames() {
        underTest.init();
        assertThat(underTest.getAllServiceNames()).containsExactlyInAnyOrder(
                "ALL",
                "ATLAS_SERVER",
                "CM-API",
                "CM-UI",
                "CRUISE_CONTROL_SERVER",
                "DAS_WEBAPP",
                "FLINK_HISTORY_SERVER",
                "HBASERESTSERVER",
                "HIVESERVER2",
                "HUE_LOAD_BALANCER",
                "IMPALAD",
                "IMPALA_DEBUG_UI",
                "JOBHISTORY",
                "KAFKA_CONNECT",
                "KUDU_MASTER",
                "LIVY_SERVER",
                "LIVY_SERVER_FOR_SPARK3",
                "MASTER",
                "MATERIALIZED_VIEW_ENGINE",
                "NAMENODE",
                "NIFI_NODE",
                "NIFI_REGISTRY_SERVER",
                "OOZIE_SERVER",
                "PHOENIX_QUERY_SERVER",
                "DATA_DISCOVERY_SERVICE_AGENT",
                "PROFILER_ADMIN_AGENT",
                "PROFILER_METRICS_AGENT",
                "PROFILER_SCHEDULER_AGENT",
                "RANGER_ADMIN",
                "RESOURCEMANAGER",
                "SCHEMA_REGISTRY_SERVER",
                "SOLR_SERVER",
                "SPARK_YARN_HISTORY_SERVER",
                "SPARK3_YARN_HISTORY_SERVER",
                "STREAMING_SQL_CONSOLE",
                "STREAMING_SQL_ENGINE",
                "STREAMS_MESSAGING_MANAGER_SERVER",
                "STREAMS_MESSAGING_MANAGER_UI",
                "ZEPPELIN_SERVER",
                "QUEUEMANAGER_WEBAPP",
                "KNOX");
    }

    @Test
    void getKnoxExposedServicesNames() {
        underTest.init();
        assertThat(underTest.getAllKnoxExposed(Optional.empty())).containsExactlyInAnyOrder(
                "ATLAS",
                "ATLAS_API",
                "AVATICA",
                "CM-API",
                "CM-UI",
                "CRUISE-CONTROL",
                "DAS",
                "FLINK",
                "HBASEJARS",
                "HBASEUI",
                "HDFSUI",
                "HIVE",
                "HUE",
                "IMPALA",
                "IMPALA_DEBUG_UI",
                "JOBHISTORYUI",
                "JOBTRACKER",
                "RESOURCEMANAGERAPI",
                "KAFKA_CONNECT",
                "KUDUUI",
                "LIVYSERVER1",
                "LIVYSERVER_API",
                "LIVY_FOR_SPARK3",
                "LIVY_FOR_SPARK3_API",
                "NAMENODE",
                "NIFI",
                "NIFI-REGISTRY",
                "NIFI_REST",
                "NIFI-REGISTRY-REST",
                "OOZIE",
                "DATA-DISCOVERY-SERVICE-API",
                "PROFILER-ADMIN-API",
                "PROFILER-METRICS-API",
                "PROFILER-SCHEDULER-API",
                "RANGER",
                "SCHEMA-REGISTRY",
                "SCHEMA-REGISTRY-API",
                "SMM-API",
                "SMM-UI",
                "SOLR",
                "SPARKHISTORYUI",
                "SPARK3HISTORYUI",
                "SSB-MVE-API",
                "SSB-SSC-UI",
                "SSB-SSC-WS",
                "SSB-SSE-API",
                "WEBHBASE",
                "WEBHDFS",
                "YARNUIV2",
                "ZEPPELIN",
                "QUEUEMANAGER_WEBAPP",
                "KNOX_TOKEN_INTEGRATOR");
    }

    @Test
    void getKnoxExposedServicesNamesWiht7211() {
        underTest.init();
        assertThat(underTest.getAllKnoxExposed(Optional.of("7.2.11"))).containsExactlyInAnyOrder(
                "ATLAS",
                "ATLAS_API",
                "AVATICA",
                "CM-API",
                "CM-UI",
                "CRUISE-CONTROL",
                "DAS",
                "FLINK",
                "HBASEJARS",
                "HBASEUI",
                "HDFSUI",
                "HIVE",
                "HUE",
                "IMPALA",
                "IMPALA_DEBUG_UI",
                "JOBHISTORYUI",
                "JOBTRACKER",
                "RESOURCEMANAGERAPI",
                "KUDUUI",
                "LIVYSERVER1",
                "LIVYSERVER_API",
                "LIVY_FOR_SPARK3",
                "LIVY_FOR_SPARK3_API",
                "NAMENODE",
                "NIFI",
                "NIFI-REGISTRY",
                "NIFI_REST",
                "NIFI-REGISTRY-REST",
                "OOZIE",
                "DATA-DISCOVERY-SERVICE-API",
                "PROFILER-ADMIN-API",
                "PROFILER-METRICS-API",
                "PROFILER-SCHEDULER-API",
                "RANGER",
                "SCHEMA-REGISTRY",
                "SCHEMA-REGISTRY-API",
                "SMM-API",
                "SMM-UI",
                "SOLR",
                "SPARKHISTORYUI",
                "SPARK3HISTORYUI",
                "SSB-MVE-API",
                "SSB-SSC-UI",
                "SSB-SSC-WS",
                "WEBHBASE",
                "WEBHDFS",
                "YARNUIV2",
                "ZEPPELIN",
                "QUEUEMANAGER_WEBAPP",
                "KNOX_TOKEN_INTEGRATOR");
    }

    @Test
    void getKnoxExposedServicesNamesWiht7214() {
        underTest.init();
        assertThat(underTest.getAllKnoxExposed(Optional.of("7.2.14"))).containsExactlyInAnyOrder(
                "ATLAS",
                "ATLAS_API",
                "AVATICA",
                "CM-API",
                "CM-UI",
                "CRUISE-CONTROL",
                "DAS",
                "FLINK",
                "HBASEJARS",
                "HBASEUI",
                "HDFSUI",
                "HIVE",
                "HUE",
                "IMPALA",
                "IMPALA_DEBUG_UI",
                "JOBHISTORYUI",
                "JOBTRACKER",
                "RESOURCEMANAGERAPI",
                "KUDUUI",
                "LIVYSERVER1",
                "LIVYSERVER_API",
                "LIVY_FOR_SPARK3",
                "LIVY_FOR_SPARK3_API",
                "NAMENODE",
                "NIFI",
                "NIFI-REGISTRY",
                "NIFI_REST",
                "NIFI-REGISTRY-REST",
                "OOZIE",
                "DATA-DISCOVERY-SERVICE-API",
                "PROFILER-ADMIN-API",
                "PROFILER-METRICS-API",
                "PROFILER-SCHEDULER-API",
                "RANGER",
                "SCHEMA-REGISTRY",
                "SCHEMA-REGISTRY-API",
                "SMM-API",
                "SMM-UI",
                "SOLR",
                "SPARKHISTORYUI",
                "SPARK3HISTORYUI",
                "SSB-MVE-API",
                "SSB-SSC-UI",
                "SSB-SSC-WS",
                "WEBHBASE",
                "WEBHDFS",
                "YARNUIV2",
                "ZEPPELIN",
                "QUEUEMANAGER_WEBAPP",
                "KNOX_TOKEN_INTEGRATOR",
                "KAFKA_CONNECT",
                "SSB-SSE-API");
    }

    @Test
    void getNonTLSServicePortsPre7211() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_2_10, false)).containsOnly(
                Map.entry("ATLAS", 21000),
                Map.entry("ATLAS_API", 21000),
                Map.entry("AVATICA", 8765),
                Map.entry("CM-API", 7180),
                Map.entry("CM-UI", 7180),
                Map.entry("DAS", 30800),
                Map.entry("FLINK", 8082),
                Map.entry("HBASEUI", 16010),
                Map.entry("HBASEJARS", 16010),
                Map.entry("HDFSUI", 9870),
                Map.entry("HIVE", 10001),
                Map.entry("HUE", 8889),
                Map.entry("IMPALA", 28000),
                Map.entry("IMPALA_DEBUG_UI", 25000),
                Map.entry("JOBHISTORYUI", 19888),
                Map.entry("JOBTRACKER", 8032),
                Map.entry("RESOURCEMANAGERAPI", 8032),
                Map.entry("KUDUUI", 8051),
                Map.entry("LIVYSERVER1", 8998),
                Map.entry("LIVYSERVER_API", 8998),
                Map.entry("LIVY_FOR_SPARK3", 28998),
                Map.entry("LIVY_FOR_SPARK3_API", 28998),
                Map.entry("NAMENODE", 8020),
                Map.entry("NIFI", 8080),
                Map.entry("NIFI-REGISTRY", 18080),
                Map.entry("NIFI_REST", 8080),
                Map.entry("NIFI-REGISTRY-REST", 18080),
                Map.entry("OOZIE", 11000),
                Map.entry("DATA-DISCOVERY-SERVICE-API", 21600),
                Map.entry("PROFILER-ADMIN-API", 21700),
                Map.entry("PROFILER-METRICS-API", 21800),
                Map.entry("PROFILER-SCHEDULER-API", 21900),
                Map.entry("RANGER", 6080),
                Map.entry("SCHEMA-REGISTRY", 7788),
                Map.entry("SCHEMA-REGISTRY-API", 7788),
                Map.entry("SMM-API", 8585),
                Map.entry("SMM-UI", 9991),
                Map.entry("SOLR", 8983),
                Map.entry("SPARKHISTORYUI", 18088),
                Map.entry("SPARK3HISTORYUI", 18089),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9870),
                Map.entry("YARNUIV2", 8088),
                Map.entry("ZEPPELIN", 8885)
        );
    }

    @Test
    void getNonTLSServicePortsPre7214() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_2_12, false)).containsOnly(
                Map.entry("ATLAS", 21000),
                Map.entry("ATLAS_API", 21000),
                Map.entry("AVATICA", 8765),
                Map.entry("CM-API", 7180),
                Map.entry("CM-UI", 7180),
                Map.entry("CRUISE-CONTROL", 8899),
                Map.entry("DAS", 30800),
                Map.entry("FLINK", 18211),
                Map.entry("HBASEUI", 16010),
                Map.entry("HBASEJARS", 16010),
                Map.entry("HDFSUI", 9870),
                Map.entry("HIVE", 10001),
                Map.entry("HUE", 8889),
                Map.entry("IMPALA", 28000),
                Map.entry("IMPALA_DEBUG_UI", 25000),
                Map.entry("JOBHISTORYUI", 19888),
                Map.entry("JOBTRACKER", 8032),
                Map.entry("RESOURCEMANAGERAPI", 8032),
                Map.entry("KUDUUI", 8051),
                Map.entry("LIVYSERVER1", 8998),
                Map.entry("LIVYSERVER_API", 8998),
                Map.entry("LIVY_FOR_SPARK3", 28998),
                Map.entry("LIVY_FOR_SPARK3_API", 28998),
                Map.entry("NAMENODE", 8020),
                Map.entry("NIFI", 8080),
                Map.entry("NIFI-REGISTRY", 18080),
                Map.entry("NIFI_REST", 8080),
                Map.entry("NIFI-REGISTRY-REST", 18080),
                Map.entry("OOZIE", 11000),
                Map.entry("DATA-DISCOVERY-SERVICE-API", 21600),
                Map.entry("PROFILER-ADMIN-API", 21700),
                Map.entry("PROFILER-METRICS-API", 21800),
                Map.entry("PROFILER-SCHEDULER-API", 21900),
                Map.entry("RANGER", 6080),
                Map.entry("SCHEMA-REGISTRY", 7788),
                Map.entry("SCHEMA-REGISTRY-API", 7788),
                Map.entry("SMM-API", 8585),
                Map.entry("SMM-UI", 9991),
                Map.entry("SOLR", 8983),
                Map.entry("SPARKHISTORYUI", 18088),
                Map.entry("SPARK3HISTORYUI", 18089),
                Map.entry("SSB-SSC-UI", 18112),
                Map.entry("SSB-SSC-WS", 18112),
                Map.entry("SSB-MVE-API", 18131),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9870),
                Map.entry("YARNUIV2", 8088),
                Map.entry("ZEPPELIN", 8885)
        );
    }

    @Test
    void getNonTLSServicePorts() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_2_14, false)).containsOnly(
                Map.entry("ATLAS", 21000),
                Map.entry("ATLAS_API", 21000),
                Map.entry("AVATICA", 8765),
                Map.entry("CM-API", 7180),
                Map.entry("CM-UI", 7180),
                Map.entry("CRUISE-CONTROL", 8899),
                Map.entry("DAS", 30800),
                Map.entry("FLINK", 18211),
                Map.entry("HBASEUI", 16010),
                Map.entry("HBASEJARS", 16010),
                Map.entry("HDFSUI", 9870),
                Map.entry("HIVE", 10001),
                Map.entry("HUE", 8889),
                Map.entry("IMPALA", 28000),
                Map.entry("IMPALA_DEBUG_UI", 25000),
                Map.entry("JOBHISTORYUI", 19888),
                Map.entry("JOBTRACKER", 8032),
                Map.entry("RESOURCEMANAGERAPI", 8032),
                Map.entry("KAFKA_CONNECT", 28083),
                Map.entry("KUDUUI", 8051),
                Map.entry("LIVYSERVER1", 8998),
                Map.entry("LIVYSERVER_API", 8998),
                Map.entry("LIVY_FOR_SPARK3", 28998),
                Map.entry("LIVY_FOR_SPARK3_API", 28998),
                Map.entry("NAMENODE", 8020),
                Map.entry("NIFI", 8080),
                Map.entry("NIFI-REGISTRY", 18080),
                Map.entry("NIFI_REST", 8080),
                Map.entry("NIFI-REGISTRY-REST", 18080),
                Map.entry("OOZIE", 11000),
                Map.entry("DATA-DISCOVERY-SERVICE-API", 21600),
                Map.entry("PROFILER-ADMIN-API", 21700),
                Map.entry("PROFILER-METRICS-API", 21800),
                Map.entry("PROFILER-SCHEDULER-API", 21900),
                Map.entry("RANGER", 6080),
                Map.entry("SCHEMA-REGISTRY", 7788),
                Map.entry("SCHEMA-REGISTRY-API", 7788),
                Map.entry("SMM-API", 8585),
                Map.entry("SMM-UI", 9991),
                Map.entry("SOLR", 8983),
                Map.entry("SPARKHISTORYUI", 18088),
                Map.entry("SPARK3HISTORYUI", 18089),
                Map.entry("SSB-SSC-UI", 18112),
                Map.entry("SSB-SSC-WS", 18112),
                Map.entry("SSB-MVE-API", 18131),
                Map.entry("SSB-SSE-API", 18121),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9870),
                Map.entry("YARNUIV2", 8088),
                Map.entry("ZEPPELIN", 8885)
        );
    }

    @Test
    void getTLSServicePortsPre7211() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_2_10, true)).containsOnly(
                Map.entry("ATLAS", 31443),
                Map.entry("ATLAS_API", 31443),
                Map.entry("AVATICA", 8765),
                Map.entry("CM-API", 7183),
                Map.entry("CM-UI", 7183),
                Map.entry("DAS", 30800),
                Map.entry("FLINK", 8082),
                Map.entry("HBASEUI", 16010),
                Map.entry("HBASEJARS", 16010),
                Map.entry("HDFSUI", 9871),
                Map.entry("HIVE", 10001),
                Map.entry("HUE", 8889),
                Map.entry("IMPALA", 28000),
                Map.entry("IMPALA_DEBUG_UI", 25000),
                Map.entry("JOBHISTORYUI", 19890),
                Map.entry("JOBTRACKER", 8032),
                Map.entry("RESOURCEMANAGERAPI", 8032),
                Map.entry("KUDUUI", 8051),
                Map.entry("LIVYSERVER1", 8998),
                Map.entry("LIVYSERVER_API", 8998),
                Map.entry("LIVY_FOR_SPARK3", 28998),
                Map.entry("LIVY_FOR_SPARK3_API", 28998),
                Map.entry("NAMENODE", 8020),
                Map.entry("NIFI", 8443),
                Map.entry("NIFI-REGISTRY", 18433),
                Map.entry("NIFI_REST", 8443),
                Map.entry("NIFI-REGISTRY-REST", 18433),
                Map.entry("OOZIE", 11443),
                Map.entry("DATA-DISCOVERY-SERVICE-API", 21600),
                Map.entry("PROFILER-ADMIN-API", 21700),
                Map.entry("PROFILER-METRICS-API", 21800),
                Map.entry("PROFILER-SCHEDULER-API", 21900),
                Map.entry("RANGER", 6182),
                Map.entry("SCHEMA-REGISTRY", 7790),
                Map.entry("SCHEMA-REGISTRY-API", 7790),
                Map.entry("SMM-API", 8587),
                Map.entry("SMM-UI", 9991),
                Map.entry("SOLR", 8985),
                Map.entry("SPARKHISTORYUI", 18488),
                Map.entry("SPARK3HISTORYUI", 18489),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9871),
                Map.entry("YARNUIV2", 8090),
                Map.entry("ZEPPELIN", 8886)
        );
    }

    @Test
    void getTLSServicePortsPre7214() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_2_12, true)).containsOnly(
                Map.entry("ATLAS", 31443),
                Map.entry("ATLAS_API", 31443),
                Map.entry("AVATICA", 8765),
                Map.entry("CM-API", 7183),
                Map.entry("CM-UI", 7183),
                Map.entry("CRUISE-CONTROL", 8899),
                Map.entry("DAS", 30800),
                Map.entry("FLINK", 18211),
                Map.entry("HBASEUI", 16010),
                Map.entry("HBASEJARS", 16010),
                Map.entry("HDFSUI", 9871),
                Map.entry("HIVE", 10001),
                Map.entry("HUE", 8889),
                Map.entry("IMPALA", 28000),
                Map.entry("IMPALA_DEBUG_UI", 25000),
                Map.entry("JOBHISTORYUI", 19890),
                Map.entry("JOBTRACKER", 8032),
                Map.entry("RESOURCEMANAGERAPI", 8032),
                Map.entry("KUDUUI", 8051),
                Map.entry("LIVYSERVER1", 8998),
                Map.entry("LIVYSERVER_API", 8998),
                Map.entry("LIVY_FOR_SPARK3", 28998),
                Map.entry("LIVY_FOR_SPARK3_API", 28998),
                Map.entry("NAMENODE", 8020),
                Map.entry("NIFI", 8443),
                Map.entry("NIFI-REGISTRY", 18433),
                Map.entry("NIFI_REST", 8443),
                Map.entry("NIFI-REGISTRY-REST", 18433),
                Map.entry("OOZIE", 11443),
                Map.entry("DATA-DISCOVERY-SERVICE-API", 21600),
                Map.entry("PROFILER-ADMIN-API", 21700),
                Map.entry("PROFILER-METRICS-API", 21800),
                Map.entry("PROFILER-SCHEDULER-API", 21900),
                Map.entry("RANGER", 6182),
                Map.entry("SCHEMA-REGISTRY", 7790),
                Map.entry("SCHEMA-REGISTRY-API", 7790),
                Map.entry("SMM-API", 8587),
                Map.entry("SMM-UI", 9991),
                Map.entry("SOLR", 8985),
                Map.entry("SPARKHISTORYUI", 18488),
                Map.entry("SPARK3HISTORYUI", 18489),
                Map.entry("SSB-SSC-UI", 18112),
                Map.entry("SSB-SSC-WS", 18112),
                Map.entry("SSB-MVE-API", 18131),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9871),
                Map.entry("YARNUIV2", 8090),
                Map.entry("ZEPPELIN", 8886)
        );
    }

    @Test
    void getTLSServicePorts() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_2_14, true)).containsOnly(
                Map.entry("ATLAS", 31443),
                Map.entry("ATLAS_API", 31443),
                Map.entry("AVATICA", 8765),
                Map.entry("CM-API", 7183),
                Map.entry("CM-UI", 7183),
                Map.entry("CRUISE-CONTROL", 8899),
                Map.entry("DAS", 30800),
                Map.entry("FLINK", 18211),
                Map.entry("HBASEUI", 16010),
                Map.entry("HBASEJARS", 16010),
                Map.entry("HDFSUI", 9871),
                Map.entry("HIVE", 10001),
                Map.entry("HUE", 8889),
                Map.entry("IMPALA", 28000),
                Map.entry("IMPALA_DEBUG_UI", 25000),
                Map.entry("JOBHISTORYUI", 19890),
                Map.entry("JOBTRACKER", 8032),
                Map.entry("RESOURCEMANAGERAPI", 8032),
                Map.entry("KAFKA_CONNECT", 28085),
                Map.entry("KUDUUI", 8051),
                Map.entry("LIVYSERVER1", 8998),
                Map.entry("LIVYSERVER_API", 8998),
                Map.entry("LIVY_FOR_SPARK3", 28998),
                Map.entry("LIVY_FOR_SPARK3_API", 28998),
                Map.entry("NAMENODE", 8020),
                Map.entry("NIFI", 8443),
                Map.entry("NIFI-REGISTRY", 18433),
                Map.entry("NIFI_REST", 8443),
                Map.entry("NIFI-REGISTRY-REST", 18433),
                Map.entry("OOZIE", 11443),
                Map.entry("DATA-DISCOVERY-SERVICE-API", 21600),
                Map.entry("PROFILER-ADMIN-API", 21700),
                Map.entry("PROFILER-METRICS-API", 21800),
                Map.entry("PROFILER-SCHEDULER-API", 21900),
                Map.entry("RANGER", 6182),
                Map.entry("SCHEMA-REGISTRY", 7790),
                Map.entry("SCHEMA-REGISTRY-API", 7790),
                Map.entry("SMM-API", 8587),
                Map.entry("SMM-UI", 9991),
                Map.entry("SOLR", 8985),
                Map.entry("SPARKHISTORYUI", 18488),
                Map.entry("SPARK3HISTORYUI", 18489),
                Map.entry("SSB-SSC-UI", 18112),
                Map.entry("SSB-SSC-WS", 18112),
                Map.entry("SSB-MVE-API", 18131),
                Map.entry("SSB-SSE-API", 18121),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9871),
                Map.entry("YARNUIV2", 8090),
                Map.entry("ZEPPELIN", 8886)
        );
    }

    @Test
    void getFullListALLOnly() {
        underTest.init();
        Set<String> allOnly = underTest.getFullServiceListBasedOnList(Set.of("ALL"), Optional.empty());
        Set<String> allAndAnother = underTest.getFullServiceListBasedOnList(Set.of("ALL", "RANGER"), Optional.empty());
        assertThat(allAndAnother.size()).isGreaterThan(0);
        assertThat(allOnly).hasSameElementsAs(allAndAnother);
    }

    @Test
    void getFullListReturnsGivenListOnly() {
        underTest.init();
        Set<String> itemsOnly = underTest.getFullServiceListBasedOnList(Set.of("ATLAS", "RANGER"), Optional.empty());
        assertThat(itemsOnly).hasSize(2);
        assertThat(itemsOnly).hasSameElementsAs(Set.of("ATLAS", "RANGER"));
    }

    @Test
    void getKnoxServicesForComponentsReturnsCMServicesAndForImpalaDebugUIAsWell() {
        underTest.init();
        Collection<ExposedService> components = underTest.knoxServicesForComponents(CDH_7_2_14, Set.of("ATLAS_SERVER", "IMPALAD"));
        assertThat(components).hasSize(7);
        assertThat(components.stream().map(ExposedService::getName)).containsExactlyInAnyOrder(
                "ATLAS",
                "ATLAS_API",
                "CLOUDERA_MANAGER",
                "CLOUDERA_MANAGER_UI",
                "IMPALA",
                "IMPALA_DEBUG_UI",
                "KNOX_TOKEN_INTEGRATOR"
        );
    }
}
