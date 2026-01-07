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

    private static final Optional<String> CDH_7_2_15 = Optional.of("7.2.15");

    private static final Optional<String> CDH_7_2_16 = Optional.of("7.2.16");

    private static final Optional<String> CDH_7_2_18 = Optional.of("7.2.18");

    private static final Optional<String> CDH_7_3_1 = Optional.of("7.3.1");

    private static final Optional<String> CDH_7_3_2 = Optional.of("7.3.2");

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
        assertEquals("EFM-UI", underTest.getEfmUIService().getName());
        assertEquals("EFM-API", underTest.getEfmRestService().getName());
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
                "EFM_SERVER",
                "NIFI_NODE",
                "NIFI_REGISTRY_SERVER",
                "OPDB_AGENT",
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
                "KNOX",
                "QUERY_PROCESSOR",
                "RANGER_RAZ_SERVER",
                "KAFKA_BROKER",
                "DLM_SERVER",
                "CLO_SERVER");
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
                "EFM-API",
                "EFM-UI",
                "OPDB-AGENT",
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
                "SSB-SSE-UI",
                "SSB-SSE-WS",
                "WEBHBASE",
                "WEBHDFS",
                "YARNUIV2",
                "ZEPPELIN",
                "QUEUEMANAGER_WEBAPP",
                "KNOX_TOKEN_INTEGRATOR",
                "QUERY_PROCESSOR",
                "RANGERRAZ",
                "KAFKA_BROKER",
                "DLM",
                "LAKEHOUSE_OPTIMIZER");
    }

    @Test
    void getKnoxExposedServicesNamesWith7211() {
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
                "KNOX_TOKEN_INTEGRATOR",
                "QUERY_PROCESSOR",
                "RANGERRAZ",
                "KAFKA_BROKER");
    }

    @Test
    void getKnoxExposedServicesNamesWith7214() {
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
                "SSB-SSE-API",
                "QUERY_PROCESSOR",
                "RANGERRAZ",
                "KAFKA_BROKER");
    }

    @Test
    void getKnoxExposedServicesNamesWith7215() {
        underTest.init();
        assertThat(underTest.getAllKnoxExposed(Optional.of("7.2.15"))).containsExactlyInAnyOrder(
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
            "OPDB-AGENT",
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
            "SSB-SSE-API",
            "QUERY_PROCESSOR",
            "RANGERRAZ",
            "KAFKA_BROKER");
    }

    @Test
    void getKnoxExposedServicesNamesWith7216() {
        underTest.init();
        assertThat(underTest.getAllKnoxExposed(Optional.of("7.2.16"))).containsExactlyInAnyOrder(
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
            "OPDB-AGENT",
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
            "SSB-SSE-UI",
            "SSB-SSE-WS",
            "WEBHBASE",
            "WEBHDFS",
            "YARNUIV2",
            "ZEPPELIN",
            "QUEUEMANAGER_WEBAPP",
            "KNOX_TOKEN_INTEGRATOR",
            "KAFKA_CONNECT",
            "SSB-SSE-API",
            "QUERY_PROCESSOR",
            "RANGERRAZ",
            "KAFKA_BROKER");
    }

    @Test
    void getNonTLSServiceProtocolsPre7211() {
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
                Map.entry("SOLR", 8993),
                Map.entry("SPARKHISTORYUI", 18088),
                Map.entry("SPARK3HISTORYUI", 18089),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9870),
                Map.entry("YARNUIV2", 8088),
                Map.entry("ZEPPELIN", 8885),
                Map.entry("QUERY_PROCESSOR", 30700),
                Map.entry("RANGERRAZ", 6082),
                Map.entry("KAFKA_BROKER", 9093)
        );
    }

    @Test
    void getHTTPSServicePortsPre732() {
        underTest.init();
        assertThat(underTest.getAllServiceProtocols(CDH_7_3_2, true)).containsOnly(
                Map.entry("ATLAS", "https"),
                Map.entry("ATLAS_API", "https"),
                Map.entry("AVATICA", "https"),
                Map.entry("CM-API", "https"),
                Map.entry("CM-UI", "https"),
                Map.entry("CRUISE-CONTROL", "https"),
                Map.entry("DAS", "https"),
                Map.entry("FLINK", "https"),
                Map.entry("HBASEUI", "https"),
                Map.entry("HBASEJARS", "https"),
                Map.entry("HDFSUI", "https"),
                Map.entry("HIVE", "https"),
                Map.entry("HUE", "https"),
                Map.entry("IMPALA", "https"),
                Map.entry("IMPALA_DEBUG_UI", "https"),
                Map.entry("JOBHISTORYUI", "https"),
                Map.entry("JOBTRACKER", "https"),
                Map.entry("RESOURCEMANAGERAPI", "https"),
                Map.entry("KAFKA_CONNECT", "https"),
                Map.entry("KUDUUI", "https"),
                Map.entry("LIVYSERVER1", "https"),
                Map.entry("LIVYSERVER_API", "https"),
                Map.entry("LIVY_FOR_SPARK3", "https"),
                Map.entry("LIVY_FOR_SPARK3_API", "https"),
                Map.entry("NAMENODE", "https"),
                Map.entry("NIFI", "https"),
                Map.entry("NIFI-REGISTRY", "https"),
                Map.entry("NIFI_REST", "https"),
                Map.entry("NIFI-REGISTRY-REST", "https"),
                Map.entry("OOZIE", "https"),
                Map.entry("OPDB-AGENT", "https"),
                Map.entry("DATA-DISCOVERY-SERVICE-API", "https"),
                Map.entry("PROFILER-ADMIN-API", "https"),
                Map.entry("PROFILER-METRICS-API", "https"),
                Map.entry("PROFILER-SCHEDULER-API", "https"),
                Map.entry("RANGER", "https"),
                Map.entry("SCHEMA-REGISTRY", "https"),
                Map.entry("SCHEMA-REGISTRY-API", "https"),
                Map.entry("SMM-API", "https"),
                Map.entry("SMM-UI", "https"),
                Map.entry("SOLR", "https"),
                Map.entry("SPARKHISTORYUI", "https"),
                Map.entry("SPARK3HISTORYUI", "https"),
                Map.entry("SSB-MVE-API", "https"),
                Map.entry("SSB-SSE-API", "https"),
                Map.entry("WEBHBASE", "https"),
                Map.entry("WEBHDFS", "https"),
                Map.entry("YARNUIV2", "https"),
                Map.entry("QUERY_PROCESSOR", "https"),
                Map.entry("RANGERRAZ", "https"),
                Map.entry("KAFKA_BROKER", "https"),
                Map.entry("EFM-UI", "https"),
                Map.entry("EFM-API", "https"),
                Map.entry("SSB-SSE-UI", "https"),
                Map.entry("SSB-SSE-WS", "https"),
                Map.entry("LAKEHOUSE_OPTIMIZER", "https")
        );
    }

    @Test
    void getHTTPSServicePortsPre731() {
        underTest.init();
        assertThat(underTest.getAllServiceProtocols(CDH_7_3_1, true)).containsOnly(
                Map.entry("ATLAS", "https"),
                Map.entry("ATLAS_API", "https"),
                Map.entry("AVATICA", "https"),
                Map.entry("CM-API", "https"),
                Map.entry("CM-UI", "https"),
                Map.entry("CRUISE-CONTROL", "https"),
                Map.entry("DAS", "https"),
                Map.entry("FLINK", "https"),
                Map.entry("HBASEUI", "https"),
                Map.entry("HBASEJARS", "https"),
                Map.entry("HDFSUI", "https"),
                Map.entry("HIVE", "https"),
                Map.entry("HUE", "https"),
                Map.entry("IMPALA", "https"),
                Map.entry("IMPALA_DEBUG_UI", "https"),
                Map.entry("JOBHISTORYUI", "https"),
                Map.entry("JOBTRACKER", "https"),
                Map.entry("RESOURCEMANAGERAPI", "https"),
                Map.entry("KAFKA_CONNECT", "https"),
                Map.entry("KUDUUI", "https"),
                Map.entry("LIVYSERVER1", "https"),
                Map.entry("LIVYSERVER_API", "https"),
                Map.entry("LIVY_FOR_SPARK3", "https"),
                Map.entry("LIVY_FOR_SPARK3_API", "https"),
                Map.entry("NAMENODE", "https"),
                Map.entry("NIFI", "https"),
                Map.entry("NIFI-REGISTRY", "https"),
                Map.entry("NIFI_REST", "https"),
                Map.entry("NIFI-REGISTRY-REST", "https"),
                Map.entry("OOZIE", "https"),
                Map.entry("OPDB-AGENT", "https"),
                Map.entry("DATA-DISCOVERY-SERVICE-API", "https"),
                Map.entry("PROFILER-ADMIN-API", "https"),
                Map.entry("PROFILER-METRICS-API", "https"),
                Map.entry("PROFILER-SCHEDULER-API", "https"),
                Map.entry("RANGER", "https"),
                Map.entry("SCHEMA-REGISTRY", "https"),
                Map.entry("SCHEMA-REGISTRY-API", "https"),
                Map.entry("SMM-API", "https"),
                Map.entry("SMM-UI", "https"),
                Map.entry("SOLR", "https"),
                Map.entry("SPARKHISTORYUI", "https"),
                Map.entry("SPARK3HISTORYUI", "https"),
                Map.entry("SSB-MVE-API", "https"),
                Map.entry("SSB-SSE-API", "https"),
                Map.entry("WEBHBASE", "https"),
                Map.entry("WEBHDFS", "https"),
                Map.entry("YARNUIV2", "https"),
                Map.entry("QUERY_PROCESSOR", "https"),
                Map.entry("RANGERRAZ", "https"),
                Map.entry("KAFKA_BROKER", "https"),
                Map.entry("EFM-UI", "https"),
                Map.entry("EFM-API", "https"),
                Map.entry("SSB-SSE-UI", "https"),
                Map.entry("SSB-SSE-WS", "https"),
                Map.entry("LAKEHOUSE_OPTIMIZER", "http")
        );
    }

    @Test
    void getHTTPSServicePortsPre7218() {
        underTest.init();
        assertThat(underTest.getAllServiceProtocols(CDH_7_2_18, true)).containsOnly(
                Map.entry("ATLAS", "https"),
                Map.entry("ATLAS_API", "https"),
                Map.entry("AVATICA", "https"),
                Map.entry("CM-API", "https"),
                Map.entry("CM-UI", "https"),
                Map.entry("CRUISE-CONTROL", "https"),
                Map.entry("DAS", "https"),
                Map.entry("FLINK", "https"),
                Map.entry("HBASEUI", "https"),
                Map.entry("HBASEJARS", "https"),
                Map.entry("HDFSUI", "https"),
                Map.entry("HIVE", "https"),
                Map.entry("HUE", "https"),
                Map.entry("IMPALA", "https"),
                Map.entry("IMPALA_DEBUG_UI", "https"),
                Map.entry("JOBHISTORYUI", "https"),
                Map.entry("JOBTRACKER", "https"),
                Map.entry("RESOURCEMANAGERAPI", "https"),
                Map.entry("KAFKA_CONNECT", "https"),
                Map.entry("KUDUUI", "https"),
                Map.entry("LIVYSERVER1", "https"),
                Map.entry("LIVYSERVER_API", "https"),
                Map.entry("LIVY_FOR_SPARK3", "https"),
                Map.entry("LIVY_FOR_SPARK3_API", "https"),
                Map.entry("NAMENODE", "https"),
                Map.entry("NIFI", "https"),
                Map.entry("NIFI-REGISTRY", "https"),
                Map.entry("NIFI_REST", "https"),
                Map.entry("NIFI-REGISTRY-REST", "https"),
                Map.entry("OOZIE", "https"),
                Map.entry("OPDB-AGENT", "https"),
                Map.entry("DATA-DISCOVERY-SERVICE-API", "https"),
                Map.entry("PROFILER-ADMIN-API", "https"),
                Map.entry("PROFILER-METRICS-API", "https"),
                Map.entry("PROFILER-SCHEDULER-API", "https"),
                Map.entry("RANGER", "https"),
                Map.entry("SCHEMA-REGISTRY", "https"),
                Map.entry("SCHEMA-REGISTRY-API", "https"),
                Map.entry("SMM-API", "https"),
                Map.entry("SMM-UI", "https"),
                Map.entry("SOLR", "https"),
                Map.entry("SPARKHISTORYUI", "https"),
                Map.entry("SPARK3HISTORYUI", "https"),
                Map.entry("SSB-MVE-API", "https"),
                Map.entry("SSB-SSE-API", "https"),
                Map.entry("WEBHBASE", "https"),
                Map.entry("WEBHDFS", "https"),
                Map.entry("YARNUIV2", "https"),
                Map.entry("ZEPPELIN", "https"),
                Map.entry("QUERY_PROCESSOR", "https"),
                Map.entry("RANGERRAZ", "https"),
                Map.entry("KAFKA_BROKER", "https"),
                Map.entry("EFM-UI", "https"),
                Map.entry("EFM-API", "https"),
                Map.entry("SSB-SSE-UI", "https"),
                Map.entry("SSB-SSE-WS", "https"),
                Map.entry("DLM", "http")
        );
    }

    @Test
    void getHTTPServicePortsPre732() {
        underTest.init();
        assertThat(underTest.getAllServiceProtocols(CDH_7_3_2, false)).containsOnly(
                Map.entry("ATLAS", "http"),
                Map.entry("ATLAS_API", "http"),
                Map.entry("AVATICA", "http"),
                Map.entry("CM-API", "http"),
                Map.entry("CM-UI", "http"),
                Map.entry("CRUISE-CONTROL", "http"),
                Map.entry("DAS", "http"),
                Map.entry("FLINK", "http"),
                Map.entry("HBASEUI", "http"),
                Map.entry("HBASEJARS", "http"),
                Map.entry("HDFSUI", "http"),
                Map.entry("HIVE", "http"),
                Map.entry("HUE", "http"),
                Map.entry("IMPALA", "http"),
                Map.entry("IMPALA_DEBUG_UI", "http"),
                Map.entry("JOBHISTORYUI", "http"),
                Map.entry("JOBTRACKER", "http"),
                Map.entry("RESOURCEMANAGERAPI", "http"),
                Map.entry("KAFKA_CONNECT", "http"),
                Map.entry("KUDUUI", "http"),
                Map.entry("LIVYSERVER1", "http"),
                Map.entry("LIVYSERVER_API", "http"),
                Map.entry("LIVY_FOR_SPARK3", "http"),
                Map.entry("LIVY_FOR_SPARK3_API", "http"),
                Map.entry("NAMENODE", "http"),
                Map.entry("NIFI", "http"),
                Map.entry("NIFI-REGISTRY", "http"),
                Map.entry("NIFI_REST", "http"),
                Map.entry("NIFI-REGISTRY-REST", "http"),
                Map.entry("OOZIE", "http"),
                Map.entry("OPDB-AGENT", "http"),
                Map.entry("DATA-DISCOVERY-SERVICE-API", "http"),
                Map.entry("PROFILER-ADMIN-API", "http"),
                Map.entry("PROFILER-METRICS-API", "http"),
                Map.entry("PROFILER-SCHEDULER-API", "http"),
                Map.entry("RANGER", "http"),
                Map.entry("SCHEMA-REGISTRY", "http"),
                Map.entry("SCHEMA-REGISTRY-API", "http"),
                Map.entry("SMM-API", "http"),
                Map.entry("SMM-UI", "http"),
                Map.entry("SOLR", "http"),
                Map.entry("SPARKHISTORYUI", "http"),
                Map.entry("SPARK3HISTORYUI", "http"),
                Map.entry("SSB-MVE-API", "http"),
                Map.entry("SSB-SSE-API", "http"),
                Map.entry("WEBHBASE", "http"),
                Map.entry("WEBHDFS", "http"),
                Map.entry("YARNUIV2", "http"),
                Map.entry("QUERY_PROCESSOR", "http"),
                Map.entry("RANGERRAZ", "http"),
                Map.entry("KAFKA_BROKER", "http"),
                Map.entry("EFM-UI", "http"),
                Map.entry("EFM-API", "http"),
                Map.entry("SSB-SSE-UI", "http"),
                Map.entry("SSB-SSE-WS", "http"),
                Map.entry("LAKEHOUSE_OPTIMIZER", "http")
        );
    }

    @Test
    void getHTTPServicePortsPre731() {
        underTest.init();
        assertThat(underTest.getAllServiceProtocols(CDH_7_3_1, false)).containsOnly(
                Map.entry("ATLAS", "http"),
                Map.entry("ATLAS_API", "http"),
                Map.entry("AVATICA", "http"),
                Map.entry("CM-API", "http"),
                Map.entry("CM-UI", "http"),
                Map.entry("CRUISE-CONTROL", "http"),
                Map.entry("DAS", "http"),
                Map.entry("FLINK", "http"),
                Map.entry("HBASEUI", "http"),
                Map.entry("HBASEJARS", "http"),
                Map.entry("HDFSUI", "http"),
                Map.entry("HIVE", "http"),
                Map.entry("HUE", "http"),
                Map.entry("IMPALA", "http"),
                Map.entry("IMPALA_DEBUG_UI", "http"),
                Map.entry("JOBHISTORYUI", "http"),
                Map.entry("JOBTRACKER", "http"),
                Map.entry("RESOURCEMANAGERAPI", "http"),
                Map.entry("KAFKA_CONNECT", "http"),
                Map.entry("KUDUUI", "http"),
                Map.entry("LIVYSERVER1", "http"),
                Map.entry("LIVYSERVER_API", "http"),
                Map.entry("LIVY_FOR_SPARK3", "http"),
                Map.entry("LIVY_FOR_SPARK3_API", "http"),
                Map.entry("NAMENODE", "http"),
                Map.entry("NIFI", "http"),
                Map.entry("NIFI-REGISTRY", "http"),
                Map.entry("NIFI_REST", "http"),
                Map.entry("NIFI-REGISTRY-REST", "http"),
                Map.entry("OOZIE", "http"),
                Map.entry("OPDB-AGENT", "http"),
                Map.entry("DATA-DISCOVERY-SERVICE-API", "http"),
                Map.entry("PROFILER-ADMIN-API", "http"),
                Map.entry("PROFILER-METRICS-API", "http"),
                Map.entry("PROFILER-SCHEDULER-API", "http"),
                Map.entry("RANGER", "http"),
                Map.entry("SCHEMA-REGISTRY", "http"),
                Map.entry("SCHEMA-REGISTRY-API", "http"),
                Map.entry("SMM-API", "http"),
                Map.entry("SMM-UI", "http"),
                Map.entry("SOLR", "http"),
                Map.entry("SPARKHISTORYUI", "http"),
                Map.entry("SPARK3HISTORYUI", "http"),
                Map.entry("SSB-MVE-API", "http"),
                Map.entry("SSB-SSE-API", "http"),
                Map.entry("WEBHBASE", "http"),
                Map.entry("WEBHDFS", "http"),
                Map.entry("YARNUIV2", "http"),
                Map.entry("QUERY_PROCESSOR", "http"),
                Map.entry("RANGERRAZ", "http"),
                Map.entry("KAFKA_BROKER", "http"),
                Map.entry("EFM-UI", "http"),
                Map.entry("EFM-API", "http"),
                Map.entry("SSB-SSE-UI", "http"),
                Map.entry("SSB-SSE-WS", "http"),
                Map.entry("LAKEHOUSE_OPTIMIZER", "http")
        );
    }

    @Test
    void getHTTPServicePortsPre7218() {
        underTest.init();
        assertThat(underTest.getAllServiceProtocols(CDH_7_2_18, false)).containsOnly(
                Map.entry("ATLAS", "http"),
                Map.entry("ATLAS_API", "http"),
                Map.entry("AVATICA", "http"),
                Map.entry("CM-API", "http"),
                Map.entry("CM-UI", "http"),
                Map.entry("CRUISE-CONTROL", "http"),
                Map.entry("DAS", "http"),
                Map.entry("FLINK", "http"),
                Map.entry("HBASEUI", "http"),
                Map.entry("HBASEJARS", "http"),
                Map.entry("HDFSUI", "http"),
                Map.entry("HIVE", "http"),
                Map.entry("HUE", "http"),
                Map.entry("IMPALA", "http"),
                Map.entry("IMPALA_DEBUG_UI", "http"),
                Map.entry("JOBHISTORYUI", "http"),
                Map.entry("JOBTRACKER", "http"),
                Map.entry("RESOURCEMANAGERAPI", "http"),
                Map.entry("KAFKA_CONNECT", "http"),
                Map.entry("KUDUUI", "http"),
                Map.entry("LIVYSERVER1", "http"),
                Map.entry("LIVYSERVER_API", "http"),
                Map.entry("LIVY_FOR_SPARK3", "http"),
                Map.entry("LIVY_FOR_SPARK3_API", "http"),
                Map.entry("NAMENODE", "http"),
                Map.entry("NIFI", "http"),
                Map.entry("NIFI-REGISTRY", "http"),
                Map.entry("NIFI_REST", "http"),
                Map.entry("NIFI-REGISTRY-REST", "http"),
                Map.entry("OOZIE", "http"),
                Map.entry("OPDB-AGENT", "http"),
                Map.entry("DATA-DISCOVERY-SERVICE-API", "http"),
                Map.entry("PROFILER-ADMIN-API", "http"),
                Map.entry("PROFILER-METRICS-API", "http"),
                Map.entry("PROFILER-SCHEDULER-API", "http"),
                Map.entry("RANGER", "http"),
                Map.entry("SCHEMA-REGISTRY", "http"),
                Map.entry("SCHEMA-REGISTRY-API", "http"),
                Map.entry("SMM-API", "http"),
                Map.entry("SMM-UI", "http"),
                Map.entry("SOLR", "http"),
                Map.entry("SPARKHISTORYUI", "http"),
                Map.entry("SPARK3HISTORYUI", "http"),
                Map.entry("SSB-MVE-API", "http"),
                Map.entry("SSB-SSE-API", "http"),
                Map.entry("WEBHBASE", "http"),
                Map.entry("WEBHDFS", "http"),
                Map.entry("YARNUIV2", "http"),
                Map.entry("ZEPPELIN", "http"),
                Map.entry("QUERY_PROCESSOR", "http"),
                Map.entry("RANGERRAZ", "http"),
                Map.entry("KAFKA_BROKER", "http"),
                Map.entry("EFM-UI", "http"),
                Map.entry("EFM-API", "http"),
                Map.entry("SSB-SSE-UI", "http"),
                Map.entry("SSB-SSE-WS", "http"),
                Map.entry("DLM", "http")
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
                Map.entry("SOLR", 8993),
                Map.entry("SPARKHISTORYUI", 18088),
                Map.entry("SPARK3HISTORYUI", 18089),
                Map.entry("SSB-SSC-UI", 18112),
                Map.entry("SSB-SSC-WS", 18112),
                Map.entry("SSB-MVE-API", 18131),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9870),
                Map.entry("YARNUIV2", 8088),
                Map.entry("ZEPPELIN", 8885),
                Map.entry("QUERY_PROCESSOR", 30700),
                Map.entry("RANGERRAZ", 6082),
                Map.entry("KAFKA_BROKER", 9093)
        );
    }

    @Test
    void getNonTLSServicePortsPre7216() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_2_15, false)).containsOnly(
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
                Map.entry("OPDB-AGENT", 8181),
                Map.entry("DATA-DISCOVERY-SERVICE-API", 21600),
                Map.entry("PROFILER-ADMIN-API", 21700),
                Map.entry("PROFILER-METRICS-API", 21800),
                Map.entry("PROFILER-SCHEDULER-API", 21900),
                Map.entry("RANGER", 6080),
                Map.entry("SCHEMA-REGISTRY", 7788),
                Map.entry("SCHEMA-REGISTRY-API", 7788),
                Map.entry("SMM-API", 8585),
                Map.entry("SMM-UI", 9991),
                Map.entry("SOLR", 8993),
                Map.entry("SPARKHISTORYUI", 18088),
                Map.entry("SPARK3HISTORYUI", 18089),
                Map.entry("SSB-SSC-UI", 18112),
                Map.entry("SSB-SSC-WS", 18112),
                Map.entry("SSB-MVE-API", 18131),
                Map.entry("SSB-SSE-API", 18121),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9870),
                Map.entry("YARNUIV2", 8088),
                Map.entry("ZEPPELIN", 8885),
                Map.entry("QUERY_PROCESSOR", 30700),
                Map.entry("RANGERRAZ", 6082),
                Map.entry("KAFKA_BROKER", 9093)
        );
    }

    @Test
    void getNonTLSServicePorts() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_2_16, false)).containsOnly(
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
                Map.entry("OPDB-AGENT", 8181),
                Map.entry("DATA-DISCOVERY-SERVICE-API", 21600),
                Map.entry("PROFILER-ADMIN-API", 21700),
                Map.entry("PROFILER-METRICS-API", 21800),
                Map.entry("PROFILER-SCHEDULER-API", 21900),
                Map.entry("RANGER", 6080),
                Map.entry("SCHEMA-REGISTRY", 7788),
                Map.entry("SCHEMA-REGISTRY-API", 7788),
                Map.entry("SMM-API", 8585),
                Map.entry("SMM-UI", 9991),
                Map.entry("SOLR", 8993),
                Map.entry("SPARKHISTORYUI", 18088),
                Map.entry("SPARK3HISTORYUI", 18089),
                Map.entry("SSB-SSE-UI", 18121),
                Map.entry("SSB-SSE-WS", 18121),
                Map.entry("SSB-MVE-API", 18131),
                Map.entry("SSB-SSE-API", 18121),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9870),
                Map.entry("YARNUIV2", 8088),
                Map.entry("ZEPPELIN", 8885),
                Map.entry("QUERY_PROCESSOR", 30700),
                Map.entry("RANGERRAZ", 6082),
                Map.entry("KAFKA_BROKER", 9093)
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
                Map.entry("ZEPPELIN", 8886),
                Map.entry("QUERY_PROCESSOR", 30700),
                Map.entry("RANGERRAZ", 6082),
                Map.entry("KAFKA_BROKER", 9093)
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
                Map.entry("ZEPPELIN", 8886),
                Map.entry("QUERY_PROCESSOR", 30700),
                Map.entry("RANGERRAZ", 6082),
                Map.entry("KAFKA_BROKER", 9093)
        );
    }

    @Test
    void getTLSServicePortsPre7216() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_2_15, true)).containsOnly(
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
                Map.entry("OPDB-AGENT", 8181),
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
                Map.entry("ZEPPELIN", 8886),
                Map.entry("QUERY_PROCESSOR", 30700),
                Map.entry("RANGERRAZ", 6082),
                Map.entry("KAFKA_BROKER", 9093)
        );
    }

    @Test
    void getTLSServicePorts7218() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_2_18, true)).containsOnly(
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
                Map.entry("OPDB-AGENT", 8181),
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
                Map.entry("SSB-MVE-API", 18131),
                Map.entry("SSB-SSE-API", 18121),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9871),
                Map.entry("YARNUIV2", 8090),
                Map.entry("ZEPPELIN", 8886),
                Map.entry("QUERY_PROCESSOR", 30700),
                Map.entry("RANGERRAZ", 6082),
                Map.entry("KAFKA_BROKER", 9093),
                Map.entry("EFM-UI", 10090),
                Map.entry("EFM-API", 10090),
                Map.entry("SSB-SSE-UI", 18121),
                Map.entry("SSB-SSE-WS", 18121),
                Map.entry("DLM", 8085)
        );
    }

    @Test
    void getTLSServicePorts731() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_3_1, true)).containsOnly(
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
                Map.entry("OPDB-AGENT", 8181),
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
                Map.entry("SSB-MVE-API", 18131),
                Map.entry("SSB-SSE-API", 18121),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9871),
                Map.entry("YARNUIV2", 8090),
                Map.entry("QUERY_PROCESSOR", 30700),
                Map.entry("RANGERRAZ", 6082),
                Map.entry("KAFKA_BROKER", 9093),
                Map.entry("EFM-UI", 10090),
                Map.entry("EFM-API", 10090),
                Map.entry("SSB-SSE-UI", 18121),
                Map.entry("SSB-SSE-WS", 18121),
                Map.entry("LAKEHOUSE_OPTIMIZER", 8085)
        );
    }

    @Test
    void getTLSServicePorts732() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_3_2, true)).containsOnly(
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
                Map.entry("OPDB-AGENT", 8181),
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
                Map.entry("SSB-MVE-API", 18131),
                Map.entry("SSB-SSE-API", 18121),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9871),
                Map.entry("YARNUIV2", 8090),
                Map.entry("QUERY_PROCESSOR", 30700),
                Map.entry("RANGERRAZ", 6082),
                Map.entry("KAFKA_BROKER", 9093),
                Map.entry("EFM-UI", 10090),
                Map.entry("EFM-API", 10090),
                Map.entry("SSB-SSE-UI", 18121),
                Map.entry("SSB-SSE-WS", 18121),
                Map.entry("LAKEHOUSE_OPTIMIZER", 8087)
        );
    }

    @Test
    void getTLSServicePorts() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(CDH_7_2_16, true)).containsOnly(
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
                Map.entry("OPDB-AGENT", 8181),
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
                Map.entry("SSB-SSE-UI", 18121),
                Map.entry("SSB-SSE-WS", 18121),
                Map.entry("SSB-MVE-API", 18131),
                Map.entry("SSB-SSE-API", 18121),
                Map.entry("WEBHBASE", 20550),
                Map.entry("WEBHDFS", 9871),
                Map.entry("YARNUIV2", 8090),
                Map.entry("ZEPPELIN", 8886),
                Map.entry("QUERY_PROCESSOR", 30700),
                Map.entry("RANGERRAZ", 6082),
                Map.entry("KAFKA_BROKER", 9093)
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
        Collection<ExposedService> components = underTest.knoxServicesForComponents(CDH_7_2_16, Set.of("ATLAS_SERVER", "IMPALAD"));
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
