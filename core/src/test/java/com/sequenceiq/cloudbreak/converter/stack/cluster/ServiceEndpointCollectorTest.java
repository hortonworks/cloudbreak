package com.sequenceiq.cloudbreak.converter.stack.cluster;

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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.ClusterExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.ExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.blueprint.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayTopologyJsonValidator;
import com.sequenceiq.cloudbreak.converter.stack.cluster.gateway.GatewayTopologyJsonToGatewayTopologyConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class ServiceEndpointCollectorTest {

    private static final String AMBARI_IP = "127.0.0.1";

    private static final String GATEWAY_PATH = "gateway-path";

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Mock
    private BlueprintValidator mockBpValidator;

    @Mock
    private StackServiceComponentDescriptors mockStackDescriptors;

    @InjectMocks
    private final ServiceEndpointCollector underTest = new ServiceEndpointCollector();

    @Mock
    private GatewayTopologyJsonValidator gatewayTopologyJsonValidator;

    @InjectMocks
    private final GatewayTopologyJsonToGatewayTopologyConverter topologyConverter = new GatewayTopologyJsonToGatewayTopologyConverter();

    @Before
    public void setup() {
        when(gatewayTopologyJsonValidator.validate(any(GatewayTopologyJson.class)))
                .thenReturn(ValidationResult.builder().build());
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
        assertEquals("https://127.0.0.1/ambari/", result);
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
        GatewayTopology topology2 = gatewayTopology("topology2", BEACON_SERVER, HIVE_SERVER, WEBHDFS);
        cluster.getGateway().setTopologies(Sets.newHashSet(topology1, topology2));
        cluster.getGateway().setGatewayType(GatewayType.INDIVIDUAL);

        Map<String, Collection<ClusterExposedServiceResponse>> clusterExposedServicesMap =
                underTest.prepareClusterExposedServices(cluster, "10.0.0.1");

        assertEquals(2, clusterExposedServicesMap.keySet().size());
        Collection<ClusterExposedServiceResponse> topology2ClusterExposedServiceResponses = clusterExposedServicesMap.get(topology2.getTopologyName());
        Optional<ClusterExposedServiceResponse> webHDFS =
                topology2ClusterExposedServiceResponses.stream().filter(service -> "WEBHDFS".equals(service.getKnoxService())).findFirst();
        if (webHDFS.isPresent()) {
            assertEquals("https://10.0.0.1:8443/gateway-path/topology2/webhdfs/", webHDFS.get().getServiceUrl());
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
            assertEquals("Spark History Server", sparkHistoryUI.get().getDisplayName());
            assertEquals("SPARK_JOBHISTORYSERVER", sparkHistoryUI.get().getServiceName());
            assertFalse(sparkHistoryUI.get().isOpen());
        } else {
            Assert.fail("no SPARKHISTORYUI in returned exposed services for topology2");
        }
    }

    @Test
    public void testGetKnoxServices() {
        when(blueprintService.getByName(any(), any())).thenReturn(new Blueprint());
        BlueprintTextProcessor blueprintTextProcessor = mock(BlueprintTextProcessor.class);
        when(blueprintProcessorFactory.get(any())).thenReturn(blueprintTextProcessor);
        when(blueprintTextProcessor.getAllComponents()).thenReturn(new HashSet<>(Arrays.asList("HIVE", "PIG")));
        when(blueprintTextProcessor.getStackName()).thenReturn("HDF");
        when(blueprintTextProcessor.getStackVersion()).thenReturn("3.1");
        Collection<ExposedServiceResponse> exposedServiceResponses = underTest.getKnoxServices(mock(IdentityUser.class), "blueprint");
        assertEquals(0, exposedServiceResponses.size());

        when(blueprintTextProcessor.getStackName()).thenReturn("HDF");
        when(blueprintTextProcessor.getStackVersion()).thenReturn("3.2");
        exposedServiceResponses = underTest.getKnoxServices(mock(IdentityUser.class), "blueprint");
        assertEquals(2, exposedServiceResponses.size());

        when(blueprintTextProcessor.getStackName()).thenReturn("HDP");
        when(blueprintTextProcessor.getStackVersion()).thenReturn("2.6");
        exposedServiceResponses = underTest.getKnoxServices(mock(IdentityUser.class), "blueprint");
        assertEquals(2, exposedServiceResponses.size());
    }

    private GatewayTopology gatewayTopology(String name, ExposedService... services) {
        GatewayTopologyJson topologyJson1 = new GatewayTopologyJson();
        topologyJson1.setTopologyName(name);
        topologyJson1.setExposedServices(Arrays.stream(services).map(ExposedService::getKnoxService).collect(Collectors.toList()));
        return topologyConverter.convert(topologyJson1);
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
        Blueprint blueprint = new Blueprint();
        try {
            String testBlueprint = FileReaderUtils.readFileFromClasspath("defaults/blueprints/hdp26-data-science-spark2-text.bp");
            blueprint.setBlueprintText(testBlueprint);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cluster.setBlueprint(blueprint);
        return cluster;
    }
}