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
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@ExtendWith(MockitoExtension.class)
class ExposedServiceCollectorTest {

    @InjectMocks
    private ExposedServiceCollector underTest;

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

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
                "ATLAS_SERVER",
                "CM-API",
                "CM-UI",
                "DAS_WEBAPP",
                "HBASERESTSERVER",
                "HIVESERVER2",
                "HUE_LOAD_BALANCER",
                "IMPALAD",
                "IMPALA_DEBUG_UI",
                "JOBHISTORY",
                "KUDU_MASTER",
                "LIVY_SERVER",
                "LIVY_SERVER",
                "MASTER",
                "NAMENODE",
                "NAMENODE",
                "NAMENODE",
                "NIFI_NODE",
                "NIFI_NODE",
                "NIFI_REGISTRY_SERVER",
                "NIFI_REGISTRY_SERVER",
                "OOZIE_SERVER",
                "PHOENIX_QUERY_SERVER",
                "DATA_DISCOVERY_SERVICE_AGENT",
                "PROFILER_ADMIN_AGENT",
                "PROFILER_METRICS_AGENT",
                "PROFILER_SCHEDULER_AGENT",
                "RANGER_ADMIN",
                "RESOURCEMANAGER",
                "RESOURCEMANAGER",
                "SCHEMA_REGISTRY_SERVER",
                "SOLR_SERVER",
                "SPARK_YARN_HISTORY_SERVER",
                "SPARK3_YARN_HISTORY_SERVER",
                "STREAMS_MESSAGING_MANAGER_SERVER",
                "STREAMS_MESSAGING_MANAGER_UI",
                "ZEPPELIN_SERVER",
                "QUEUEMANAGER_WEBAPP");
    }

    @Test
    void geKnoxExposedServicesNames() {
        underTest.init();
        assertThat(underTest.getAllKnoxExposed()).containsExactlyInAnyOrder(
                "ATLAS",
                "ATLAS_API",
                "AVATICA",
                "CM-API",
                "CM-UI",
                "DAS",
                "HBASEUI",
                "HDFSUI",
                "HIVE",
                "HUE",
                "IMPALA",
                "IMPALA_DEBUG_UI",
                "JOBHISTORYUI",
                "JOBTRACKER",
                "KUDUUI",
                "LIVYSERVER1",
                "LIVYSERVER_API",
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
                "SMM-API",
                "SMM-UI",
                "SOLR",
                "SPARKHISTORYUI",
                "SPARK3HISTORYUI",
                "WEBHBASE",
                "WEBHDFS",
                "YARNUIV2",
                "ZEPPELIN",
                "QUEUEMANAGER_WEBAPP");
    }

    @Test
    void getNonTLSServicePorts() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(false)).containsOnly(
                Map.entry("ATLAS", 21000),
                Map.entry("ATLAS_API", 21000),
                Map.entry("AVATICA", 8765),
                Map.entry("CM-API", 7180),
                Map.entry("CM-UI", 7180),
                Map.entry("DAS", 30800),
                Map.entry("HBASEUI", 16010),
                Map.entry("HDFSUI", 9870),
                Map.entry("HIVE", 10001),
                Map.entry("HUE", 8889),
                Map.entry("IMPALA", 28000),
                Map.entry("IMPALA_DEBUG_UI", 25000),
                Map.entry("JOBHISTORYUI", 19888),
                Map.entry("JOBTRACKER", 8032),
                Map.entry("KUDUUI", 8051),
                Map.entry("LIVYSERVER1", 8998),
                Map.entry("LIVYSERVER_API", 8998),
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
    void getTLSServicePorts() {
        underTest.init();
        assertThat(underTest.getAllServicePorts(true)).containsOnly(
                Map.entry("ATLAS", 31443),
                Map.entry("ATLAS_API", 31443),
                Map.entry("AVATICA", 8765),
                Map.entry("CM-API", 7183),
                Map.entry("CM-UI", 7183),
                Map.entry("DAS", 30800),
                Map.entry("HBASEUI", 16010),
                Map.entry("HDFSUI", 9871),
                Map.entry("HIVE", 10001),
                Map.entry("HUE", 8889),
                Map.entry("IMPALA", 28000),
                Map.entry("IMPALA_DEBUG_UI", 25000),
                Map.entry("JOBHISTORYUI", 19890),
                Map.entry("JOBTRACKER", 8032),
                Map.entry("KUDUUI", 8051),
                Map.entry("LIVYSERVER1", 8998),
                Map.entry("LIVYSERVER_API", 8998),
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
    void getFullListALLOnly() {
        underTest.init();
        Set<String> allOnly = underTest.getFullServiceListBasedOnList(Set.of("ALL"));
        Set<String> allAndAnother = underTest.getFullServiceListBasedOnList(Set.of("ALL", "RANGER"));
        assertThat(allAndAnother.size()).isGreaterThan(0);
        assertThat(allOnly).hasSameElementsAs(allAndAnother);
    }

    @Test
    void getFullListReturnsGivenListOnly() {
        underTest.init();
        Set<String> itemsOnly = underTest.getFullServiceListBasedOnList(Set.of("ATLAS", "RANGER"));
        assertThat(itemsOnly).hasSize(2);
        assertThat(itemsOnly).hasSameElementsAs(Set.of("ATLAS", "RANGER"));
    }

    @Test
    void getKnoxServicesForComponentsReturnsCMServicesAndForImpalaDebugUIAsWell() {
        underTest.init();
        Collection<ExposedService> components = underTest.knoxServicesForComponents(Set.of("ATLAS_SERVER", "IMPALAD"));
        assertThat(components).hasSize(6);
        assertThat(components.stream().map(ExposedService::getName)).containsExactlyInAnyOrder(
                "ATLAS",
                "ATLAS_API",
                "CLOUDERA_MANAGER",
                "CLOUDERA_MANAGER_UI",
                "IMPALA",
                "IMPALA_DEBUG_UI"
        );
    }
}
