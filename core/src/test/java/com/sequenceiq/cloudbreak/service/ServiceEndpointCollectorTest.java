package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService.ATLAS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService.CLOUDERA_MANAGER_UI;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService.HIVE_SERVER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService.WEBHDFS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Response;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToExposedServicesConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class ServiceEndpointCollectorTest {

    private static final String CLOUDERA_MANAGER_IP = "127.0.0.1";

    private static final String GATEWAY_PATH = "gateway-path";

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private StackServiceComponentDescriptors mockStackDescriptors;

    @Mock
    private ComponentLocatorService componentLocatorService;

    @InjectMocks
    private final ServiceEndpointCollector underTest = new ServiceEndpointCollector();

    @Mock
    private ExposedServiceListValidator exposedServiceListValidator;

    @InjectMocks
    private final GatewayTopologyV4RequestToExposedServicesConverter exposedServicesConverter = new GatewayTopologyV4RequestToExposedServicesConverter();

    @Mock
    private Workspace workspace;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(underTest, "httpsPort", "443");
        when(exposedServiceListValidator.validate(any())).thenReturn(ValidationResult.builder().build());
    }

    @Test
    public void testGetCmServerUrlWithYarnOrchestrator() {
        Cluster cluster = clusterkWithOrchestrator("YARN");
        String ambariIp = CLOUDERA_MANAGER_IP;

        String result = underTest.getManagerServerUrl(cluster, ambariIp);
        assertEquals("http://127.0.0.1:8080", result);
    }

    @Test
    public void testGetCmServerUrlWithNullGateway() {
        Cluster cluster = clusterkWithOrchestrator("ANY");
        cluster.setGateway(null);

        String result = underTest.getManagerServerUrl(cluster, CLOUDERA_MANAGER_IP);
        assertEquals("https://127.0.0.1/clouderamanager/", result);
    }

    @Test
    public void testGetCmServerUrlWithNoAmbariInTopologies() {
        Cluster cluster = createClusterWithComponents(new ExposedService[]{ATLAS, ATLAS},
                new ExposedService[]{HIVE_SERVER}, GatewayType.INDIVIDUAL);

        String result = underTest.getManagerServerUrl(cluster, CLOUDERA_MANAGER_IP);
        assertEquals("https://127.0.0.1/", result);
    }

    @Test
    public void testGetCmServerUrlWithAmbariPresentInTopologiesWithCentralGateway() {
        Cluster cluster = createClusterWithComponents(new ExposedService[]{CLOUDERA_MANAGER_UI, ATLAS},
                new ExposedService[]{HIVE_SERVER}, GatewayType.CENTRAL);

        String result = underTest.getManagerServerUrl(cluster, CLOUDERA_MANAGER_IP);
        assertEquals("/gateway-path/topology1/cmf/home/", result);
    }

    @Test
    public void testGetCmServerUrlWithAmbariPresentInTopologiesWithIndividualGateway() {
        Cluster cluster = createClusterWithComponents(new ExposedService[]{CLOUDERA_MANAGER_UI, ATLAS},
                new ExposedService[]{HIVE_SERVER}, GatewayType.INDIVIDUAL);
        cluster.getGateway().setGatewayPort(443);

        String result = underTest.getManagerServerUrl(cluster, CLOUDERA_MANAGER_IP);
        assertEquals("https://127.0.0.1/gateway-path/topology1/cmf/home/", result);
    }

    @Test
    public void testGetCmServerUrlInTopologiesWithIndividualGatewayOnPort8443() {
        Cluster cluster = createClusterWithComponents(new ExposedService[]{CLOUDERA_MANAGER_UI, ATLAS},
                new ExposedService[]{HIVE_SERVER}, GatewayType.INDIVIDUAL);
        cluster.getGateway().setGatewayPort(8443);

        String result = underTest.getManagerServerUrl(cluster, CLOUDERA_MANAGER_IP);
        assertEquals("https://127.0.0.1:8443/gateway-path/topology1/cmf/home/", result);
    }

    @Test
    public void testPrepareClusterExposedServices() {
        Cluster cluster = createClusterWithComponents(new ExposedService[]{ATLAS},
                new ExposedService[]{HIVE_SERVER, WEBHDFS}, GatewayType.INDIVIDUAL);
        cluster.getGateway().setGatewayPort(443);

        mockBlueprintTextProcessor();
        mockComponentLocator(Lists.newArrayList("10.0.0.1"));

        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesMap =
                underTest.prepareClusterExposedServices(cluster, "10.0.0.1");

        assertEquals(4L, clusterExposedServicesMap.keySet().size());

        Collection<ClusterExposedServiceV4Response> topology2ClusterExposedServiceV4Responses = clusterExposedServicesMap.get("topology2");
        Optional<ClusterExposedServiceV4Response> webHDFS =
                topology2ClusterExposedServiceV4Responses.stream().filter(service -> "WEBHDFS".equals(service.getKnoxService())).findFirst();

        if (webHDFS.isPresent()) {
            assertEquals("https://10.0.0.1/gateway-path/topology2/webhdfs/v1", webHDFS.get().getServiceUrl());
            assertEquals("WEBHDFS", webHDFS.get().getKnoxService());
            assertEquals("WebHDFS", webHDFS.get().getDisplayName());
            assertEquals("NAMENODE", webHDFS.get().getServiceName());
            assertTrue(webHDFS.get().isOpen());
        }

        Optional<ClusterExposedServiceV4Response> sparkHistoryUI =
                topology2ClusterExposedServiceV4Responses.stream().filter(service -> "SPARKHISTORYUI".equals(service.getKnoxService())).findFirst();
        if (sparkHistoryUI.isPresent()) {
            assertEquals("https://10.0.0.1/gateway-path/topology2/sparkhistory/", sparkHistoryUI.get().getServiceUrl());
            assertEquals("SPARKHISTORYUI", sparkHistoryUI.get().getKnoxService());
            assertEquals("Spark 1.x History Server", sparkHistoryUI.get().getDisplayName());
            assertEquals("SPARK_YARN_HISTORY_SERVER", sparkHistoryUI.get().getServiceName());
            assertFalse(sparkHistoryUI.get().isOpen());
        }

        Optional<ClusterExposedServiceV4Response> hiveServer =
                topology2ClusterExposedServiceV4Responses.stream().filter(service -> "HIVE".equals(service.getKnoxService())).findFirst();
        if (hiveServer.isPresent()) {
            assertEquals("jdbc:hive2://10.0.0.1/;ssl=true;sslTrustStore=/cert/gateway.jks;trustStorePassword=${GATEWAY_JKS_PASSWORD};"
                    + "transportMode=http;httpPath=gateway-path/topology2/hive", hiveServer.get().getServiceUrl());
            assertEquals("HIVE", hiveServer.get().getKnoxService());
            assertEquals("Hive Server", hiveServer.get().getDisplayName());
            assertEquals("HIVE_SERVER", hiveServer.get().getServiceName());
            assertTrue(hiveServer.get().isOpen());
        }
    }

    @Test
    public void testGetKnoxServices() {
        mockBlueprintTextProcessor();

        Collection<ExposedServiceV4Response> exposedServiceResponses = underTest.getKnoxServices(workspace.getId(), "blueprint");
        assertEquals(2L, exposedServiceResponses.size());

        mockBlueprintTextProcessor();

        exposedServiceResponses = underTest.getKnoxServices(workspace.getId(), "blueprint");
        assertEquals(2L, exposedServiceResponses.size());

        mockBlueprintTextProcessor();

        exposedServiceResponses = underTest.getKnoxServices(workspace.getId(), "blueprint");
        assertEquals(2L, exposedServiceResponses.size());
    }

    @Test
    public void testGetKnoxServicesWithLivyServer() {
        mockBlueprintTextProcessor();

        Collection<ExposedServiceV4Response> exposedServiceResponses = underTest.getKnoxServices(workspace.getId(), "blueprint");
        assertEquals(2L, exposedServiceResponses.size());

        mockBlueprintTextProcessor();

        exposedServiceResponses = underTest.getKnoxServices(workspace.getId(), "blueprint");
        assertEquals(2L, exposedServiceResponses.size());
    }

    private void mockBlueprintTextProcessor() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{\"Blueprints\":{}}");
        when(blueprintService.getByNameForWorkspaceId(any(), anyLong())).thenReturn(blueprint);
        when(blueprintService.isAmbariBlueprint(any())).thenReturn(true);
        AmbariBlueprintTextProcessor blueprintTextProcessor = mock(AmbariBlueprintTextProcessor.class);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(ambariBlueprintProcessorFactory.get(any())).thenReturn(blueprintTextProcessor);
        when(cmTemplateProcessorFactory.get(any())).thenReturn(cmTemplateProcessor);
    }

    private void mockComponentLocator(List<String> privateIps) {
        Map<String, List<String>> componentPrivateIps = Maps.newHashMap();
        componentPrivateIps.put("NAMENODE", privateIps);
        when(componentLocatorService.getComponentLocation(any(), any(), any())).thenReturn(componentPrivateIps);
    }

    private GatewayTopology gatewayTopology(String name, ExposedService... services) {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(name);
        gatewayTopologyJson.setExposedServices(Arrays.stream(services).map(ExposedService::getKnoxService).collect(Collectors.toList()));
        ExposedServices exposedServices = exposedServicesConverter.convert(gatewayTopologyJson);
        GatewayTopology gatewayTopology = new GatewayTopology();
        gatewayTopology.setTopologyName(name);
        gatewayTopology.setExposedServices(new Json(exposedServices));
        return gatewayTopology;
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

    private Cluster createClusterWithComponents(ExposedService[] topology1Services, ExposedService[] topology2Services, GatewayType gatewayType) {
        Cluster cluster = clusterkWithOrchestrator("ANY");
        GatewayTopology topology1 = gatewayTopology("topology1", topology1Services);
        topology1.setGateway(cluster.getGateway());
        GatewayTopology topology2 = gatewayTopology("topology2", topology2Services);
        topology2.setGateway(cluster.getGateway());
        cluster.getGateway().setTopologies(Sets.newHashSet(topology1, topology2));
        cluster.getGateway().setGatewayType(gatewayType);
        return cluster;
    }
}