package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.api.model.ExposedService.AMBARI;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.ATLAS;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.BEACON_SERVER;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.HIVE_SERVER;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.WEBHDFS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.ClusterExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.ExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.clusterdefinition.validation.AmbariBlueprintValidator;
import com.sequenceiq.cloudbreak.clusterdefinition.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.converter.stack.cluster.gateway.GatewayTopologyJsonToExposedServicesConverter;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class ServiceEndpointCollectorTest {

    private static final String AMBARI_IP = "127.0.0.1";

    private static final String GATEWAY_PATH = "gateway-path";

    @Mock
    private ClusterDefinitionService clusterDefinitionService;

    @Mock
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Mock
    private AmbariBlueprintValidator mockBpValidator;

    @Mock
    private StackServiceComponentDescriptors mockStackDescriptors;

    @Spy
    private AmbariHaComponentFilter ambariHaComponentFilter;

    @InjectMocks
    private final ServiceEndpointCollector underTest = new ServiceEndpointCollector();

    @Mock
    private ExposedServiceListValidator exposedServiceListValidator;

    @InjectMocks
    private final GatewayTopologyJsonToExposedServicesConverter exposedServicesConverter = new GatewayTopologyJsonToExposedServicesConverter();

    @Mock
    private Workspace workspace;

    @Before
    public void setup() {
        when(exposedServiceListValidator.validate(any())).thenReturn(ValidationResult.builder().build());
        ReflectionTestUtils.setField(underTest, "knoxPort", "8443");
    }

    @Test
    public void testGetAmbariServerUrlWithYarnOrchestrator() {
        Cluster cluster = clusterkWithOrchestrator("YARN");
        String ambariIp = AMBARI_IP;

        String result = underTest.getAmbariServerUrl(cluster, ambariIp);
        assertEquals("http://127.0.0.1:8080", result);
    }

    @Test
    public void testGetAmbariServerUrlWithNullGateway() {
        Cluster cluster = clusterkWithOrchestrator("ANY");
        cluster.setGateway(null);

        String result = underTest.getAmbariServerUrl(cluster, AMBARI_IP);
        assertEquals("https://127.0.0.1/ambari/", result);
    }

    @Test
    public void testGetAmbariServerUrlWithNoAmbariInTopologies() {
        Cluster cluster = clusterkWithOrchestrator("ANY");
        GatewayTopology topology1 = gatewayTopology("topology1", ATLAS, ATLAS);
        GatewayTopology topology2 = gatewayTopology("topology2", BEACON_SERVER, HIVE_SERVER);
        cluster.getGateway().setTopologies(Sets.newHashSet(topology1, topology2));

        String result = underTest.getAmbariServerUrl(cluster, AMBARI_IP);
        assertEquals("", result);
    }

    @Test
    public void testGetAmbariServerUrlWithAmbariPresentInTopologiesWithCentralGateway() {
        Cluster cluster = clusterkWithOrchestrator("ANY");
        GatewayTopology topology1 = gatewayTopology("topology1", AMBARI, ATLAS);
        GatewayTopology topology2 = gatewayTopology("topology2", BEACON_SERVER, HIVE_SERVER);
        cluster.getGateway().setTopologies(Sets.newHashSet(topology1, topology2));
        cluster.getGateway().setGatewayType(GatewayType.CENTRAL);

        String result = underTest.getAmbariServerUrl(cluster, AMBARI_IP);
        assertEquals("/gateway-path/topology1/ambari/", result);
    }

    @Test
    public void testGetAmbariServerUrlWithAmbariPresentInTopologiesWithIndividualGateway() {
        Cluster cluster = clusterkWithOrchestrator("ANY");
        GatewayTopology topology1 = gatewayTopology("topology1", AMBARI, ATLAS);
        GatewayTopology topology2 = gatewayTopology("topology2", BEACON_SERVER, HIVE_SERVER);
        cluster.getGateway().setTopologies(Sets.newHashSet(topology1, topology2));
        cluster.getGateway().setGatewayType(GatewayType.INDIVIDUAL);

        String result = underTest.getAmbariServerUrl(cluster, AMBARI_IP);
        assertEquals("https://127.0.0.1:8443/gateway-path/topology1/ambari/", result);
    }

    @Test
    public void testPrepareClusterExposedServices() {
        Cluster cluster = clusterkWithOrchestrator("ANY");
        GatewayTopology topology1 = gatewayTopology("topology1", AMBARI, ATLAS);
        topology1.setGateway(cluster.getGateway());
        GatewayTopology topology2 = gatewayTopology("topology2", BEACON_SERVER, HIVE_SERVER, WEBHDFS);
        topology2.setGateway(cluster.getGateway());
        cluster.getGateway().setTopologies(Sets.newHashSet(topology1, topology2));
        cluster.getGateway().setGatewayType(GatewayType.INDIVIDUAL);

        Map<String, Collection<ClusterExposedServiceResponse>> clusterExposedServicesMap =
                underTest.prepareClusterExposedServices(cluster, "10.0.0.1");

        assertEquals(2L, clusterExposedServicesMap.keySet().size());
        Collection<ClusterExposedServiceResponse> topology2ClusterExposedServiceResponses = clusterExposedServicesMap.get(topology2.getTopologyName());
        Optional<ClusterExposedServiceResponse> webHDFS =
                topology2ClusterExposedServiceResponses.stream().filter(service -> "WEBHDFS".equals(service.getKnoxService())).findFirst();
        if (webHDFS.isPresent()) {
            assertEquals("https://10.0.0.1:8443/gateway-path/topology2/webhdfs/v1", webHDFS.get().getServiceUrl());
            assertEquals("WEBHDFS", webHDFS.get().getKnoxService());
            assertEquals("WebHDFS", webHDFS.get().getDisplayName());
            assertEquals("NAMENODE", webHDFS.get().getServiceName());
            assertTrue(webHDFS.get().isOpen());
        } else {
            Assert.fail("no WEBHDFS in returned exposed services for topology2");
        }

        Optional<ClusterExposedServiceResponse> sparkHistoryUI =
                topology2ClusterExposedServiceResponses.stream().filter(service -> "SPARKHISTORYUI".equals(service.getKnoxService())).findFirst();
        if (sparkHistoryUI.isPresent()) {
            assertEquals("https://10.0.0.1:8443/gateway-path/topology2/sparkhistory/", sparkHistoryUI.get().getServiceUrl());
            assertEquals("SPARKHISTORYUI", sparkHistoryUI.get().getKnoxService());
            assertEquals("Spark 1.x History Server", sparkHistoryUI.get().getDisplayName());
            assertEquals("SPARK_JOBHISTORYSERVER", sparkHistoryUI.get().getServiceName());
            assertFalse(sparkHistoryUI.get().isOpen());
        } else {
            Assert.fail("no SPARKHISTORYUI in returned exposed services for topology2");
        }

        Optional<ClusterExposedServiceResponse> hiveServer =
                topology2ClusterExposedServiceResponses.stream().filter(service -> "HIVE".equals(service.getKnoxService())).findFirst();
        if (hiveServer.isPresent()) {
            assertEquals("jdbc:hive2://10.0.0.1:8443/;ssl=true;sslTrustStore=/cert/gateway.jks;trustStorePassword=${GATEWAY_JKS_PASSWORD};"
                    + "transportMode=http;httpPath=gateway-path/topology2/hive", hiveServer.get().getServiceUrl());
            assertEquals("HIVE", hiveServer.get().getKnoxService());
            assertEquals("Hive Server", hiveServer.get().getDisplayName());
            assertEquals("HIVE_SERVER", hiveServer.get().getServiceName());
            assertTrue(hiveServer.get().isOpen());
        } else {
            Assert.fail("no HIVE in returned exposed services for topology2");
        }
    }

    @Test
    public void testGetKnoxServices() {
        when(clusterDefinitionService.getByNameForWorkspace(any(), any(Workspace.class))).thenReturn(new ClusterDefinition());
        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);
        when(ambariBlueprintProcessorFactory.get(any())).thenReturn(ambariBlueprintTextProcessor);
        when(ambariBlueprintTextProcessor.getAllComponents()).thenReturn(new HashSet<>(Arrays.asList("HIVE", "PIG")));
        when(ambariBlueprintTextProcessor.getStackName()).thenReturn("HDF");
        when(ambariBlueprintTextProcessor.getStackVersion()).thenReturn("3.1");
        Collection<ExposedServiceResponse> exposedServiceResponses = underTest.getKnoxServices("blueprint", workspace);
        assertEquals(0L, exposedServiceResponses.size());

        when(ambariBlueprintTextProcessor.getStackName()).thenReturn("HDF");
        when(ambariBlueprintTextProcessor.getStackVersion()).thenReturn("3.2");
        exposedServiceResponses = underTest.getKnoxServices("blueprint", workspace);
        assertEquals(1L, exposedServiceResponses.size());

        when(ambariBlueprintTextProcessor.getStackName()).thenReturn("HDP");
        when(ambariBlueprintTextProcessor.getStackVersion()).thenReturn("2.6");
        exposedServiceResponses = underTest.getKnoxServices("blueprint", workspace);
        assertEquals(1L, exposedServiceResponses.size());
    }

    @Test
    public void testGetKnoxServicesWithLivyServerAndResourceManagerV2() {
        when(clusterDefinitionService.getByNameForWorkspace(any(), any(Workspace.class))).thenReturn(new ClusterDefinition());
        AmbariBlueprintTextProcessor ambariBlueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);
        when(ambariBlueprintProcessorFactory.get(any())).thenReturn(ambariBlueprintTextProcessor);
        when(ambariBlueprintTextProcessor.getAllComponents()).thenReturn(new HashSet<>(Arrays.asList("RESOURCEMANAGER", "LIVY2_SERVER",
                "SPARK2_JOBHISTORYSERVER", "LOGSEARCH_SERVER")));
        when(ambariBlueprintTextProcessor.getStackName()).thenReturn("HDP");
        when(ambariBlueprintTextProcessor.getStackVersion()).thenReturn("2.6");

        Collection<ExposedServiceResponse> exposedServiceResponses = underTest.getKnoxServices("blueprint", workspace);

        assertEquals(3L, exposedServiceResponses.size());
        assertFalse(createExposedServiceFilteredStream(exposedServiceResponses)
                .findFirst()
                .isPresent());

        when(ambariBlueprintTextProcessor.getStackVersion()).thenReturn("3.0");

        exposedServiceResponses = underTest.getKnoxServices("blueprint", workspace);

        assertEquals(6L, exposedServiceResponses.size());
        assertTrue(createExposedServiceFilteredStream(exposedServiceResponses)
                .count() == 2);
    }

    private Stream<ExposedServiceResponse> createExposedServiceFilteredStream(Collection<ExposedServiceResponse> exposedServiceResponses) {
        return exposedServiceResponses
                .stream()
                .filter(exposedServiceResponse -> StringUtils.equals(exposedServiceResponse.getKnoxService(), "YARNUIV2")
                        || StringUtils.equals(exposedServiceResponse.getKnoxService(), "LIVYSERVER")
                        || StringUtils.equals(exposedServiceResponse.getKnoxService(), "LOGSEARCH_SERVER"));
    }

    private GatewayTopology gatewayTopology(String name, ExposedService... services) {
        try {
            GatewayTopologyJson gatewayTopologyJson = new GatewayTopologyJson();
            gatewayTopologyJson.setTopologyName(name);
            gatewayTopologyJson.setExposedServices(Arrays.stream(services).map(ExposedService::getKnoxService).collect(Collectors.toList()));
            ExposedServices exposedServices = exposedServicesConverter.convert(gatewayTopologyJson);
            GatewayTopology gatewayTopology = new GatewayTopology();
            gatewayTopology.setTopologyName(name);
            gatewayTopology.setExposedServices(new Json(exposedServices));
            return gatewayTopology;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Cluster clusterkWithOrchestrator(String orchestratorType) {
        Cluster cluster = new Cluster();
        Stack stack = new Stack();
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType(orchestratorType);
        Gateway gateway = new Gateway();
        gateway.setPath(GATEWAY_PATH);
        stack.setOrchestrator(orchestrator);
        cluster.setStack(stack);
        cluster.setGateway(gateway);
        ClusterDefinition clusterDefinition = new ClusterDefinition();
        try {
            String testBlueprint = FileReaderUtils.readFileFromClasspath("defaults/blueprints/hdp26-data-science-spark2-text.bp");
            clusterDefinition.setClusterDefinitionText(testBlueprint);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cluster.setClusterDefinition(clusterDefinition);
        return cluster;
    }
}