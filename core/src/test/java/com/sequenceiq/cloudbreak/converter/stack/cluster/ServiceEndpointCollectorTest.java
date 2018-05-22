package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static com.sequenceiq.cloudbreak.api.model.ExposedService.AMBARI;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.ATLAS;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.BEACON_SERVER;
import static com.sequenceiq.cloudbreak.api.model.ExposedService.HIVE_SERVER;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.blueprint.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayTopologyJsonValidator;
import com.sequenceiq.cloudbreak.converter.stack.cluster.gateway.GatewayTopologyJsonToGatewayTopologyConverter;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@RunWith(MockitoJUnitRunner.class)
public class ServiceEndpointCollectorTest {

    private static final String AMBARI_IP = "127.0.0.1";

    private static final String GATEWAY_PATH = "gateway-path";

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
        return cluster;
    }
}