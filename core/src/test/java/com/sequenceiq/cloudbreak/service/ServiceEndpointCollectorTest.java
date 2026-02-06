package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceUtil.exposedService;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;
import com.sequenceiq.cloudbreak.cmtemplate.validation.StackServiceComponentDescriptors;
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
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class ServiceEndpointCollectorTest {

    private static final String CLOUDERA_MANAGER_IP = "127.0.0.1";

    private static final String GATEWAY_PATH = "gateway-path";

    @InjectMocks
    private final ServiceEndpointCollector underTest = new ServiceEndpointCollector();

    @InjectMocks
    private final GatewayTopologyV4RequestToExposedServicesConverter exposedServicesConverter = new GatewayTopologyV4RequestToExposedServicesConverter();

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private StackServiceComponentDescriptors mockStackDescriptors;

    @Mock
    private ComponentLocatorService componentLocatorService;

    @Mock
    private ExposedServiceListValidator exposedServiceListValidator;

    @Mock
    private ExposedServiceCollector exposedServiceCollector;

    @Mock
    private Workspace workspace;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ServiceEndpointCollectorEntitlementComparator serviceEndpointCollectorEntitlementComparator;

    @Mock
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Mock
    private CmTemplateGeneratorService templateGeneratorService;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(underTest, "httpsPort", "443");
        lenient().when(exposedServiceListValidator.validate(any())).thenReturn(ValidationResult.builder().build());
        lenient().when(exposedServiceCollector.getClouderaManagerUIService()).thenReturn(getClouderaManagerUIService());
        lenient().when(exposedServiceCollector.getImpalaService()).thenReturn(exposedService("IMPALA"));
        lenient().when(exposedServiceCollector.knoxServicesForComponents(any(Optional.class), anyList())).thenReturn(
                List.of(exposedService("CLOUDERA_MANAGER"), exposedService("CLOUDERA_MANAGER_UI"), exposedService("NAMENODE"), exposedService("HBASEJARS")));
        lenient().when(exposedServiceCollector.getFullServiceListBasedOnList(anyList(), any())).thenAnswer(a -> Set.copyOf(a.getArgument(0)));
        lenient().when(serviceEndpointCollectorEntitlementComparator.entitlementSupported(anyList(), eq(null))).thenReturn(true);
        lenient().when(exposedServiceCollector.getNameNodeService()).thenReturn(exposedService("NAMENODE"));
        lenient().when(exposedServiceCollector.getHBaseJarsService()).thenReturn(exposedService("HBASEJARS"));
        lenient().when(templateGeneratorService.getServicesByBlueprint(any())).thenReturn(new SupportedServices());
    }

    @Test
    public void testGetCmServerUrlWithYarnOrchestrator() {
        Stack stack = stackWithOrchestrator("YARN");

        String result = underTest.getManagerServerUrl(stack, CLOUDERA_MANAGER_IP);
        assertEquals("http://127.0.0.1:8080", result);
    }

    @Test
    public void testGetCmServerUrlWithNullGateway() {
        Stack stack = stackWithOrchestrator("ANY");
        stack.getCluster().setGateway(null);

        String result = underTest.getManagerServerUrl(stack, CLOUDERA_MANAGER_IP);
        assertEquals("https://127.0.0.1/clouderamanager/", result);
    }

    @Test
    public void testGetCmServerUrlWithNoAmbariInTopologies() {
        Stack stack = createStackWithComponents(new ExposedService[] { exposedService("ATLAS"), exposedService("ATLAS") },
                new ExposedService[] { exposedService("HIVE_SERVER") }, GatewayType.INDIVIDUAL);

        String result = underTest.getManagerServerUrl(stack, CLOUDERA_MANAGER_IP);
        assertEquals("https://127.0.0.1/", result);
    }

    @Test
    public void testGetCmServerUrlWithAmbariPresentInTopologiesWithCentralGateway() {
        Stack stack = createStackWithComponents(new ExposedService[] { exposedService("CLOUDERA_MANAGER_UI"), exposedService("ATLAS") },
                new ExposedService[] { exposedService("HIVE_SERVER") }, GatewayType.CENTRAL);

        String result = underTest.getManagerServerUrl(stack, CLOUDERA_MANAGER_IP);
        assertEquals("/gateway-path/topology1/cmf/home/", result);
    }

    @Test
    public void testGetCmServerUrlWithAmbariPresentInTopologiesWithIndividualGateway() {
        Stack stack = createStackWithComponents(new ExposedService[] { exposedService("CLOUDERA_MANAGER_UI"), exposedService("ATLAS") },
                new ExposedService[] { exposedService("HIVE_SERVER") }, GatewayType.INDIVIDUAL);
        stack.getCluster().getGateway().setGatewayPort(443);

        String result = underTest.getManagerServerUrl(stack, CLOUDERA_MANAGER_IP);
        assertEquals("https://127.0.0.1/gateway-path/topology1/cmf/home/", result);
    }

    @Test
    public void testGetCmServerUrlInTopologiesWithIndividualGatewayOnPort8443() {
        ExposedService clouderaManagerUIService = getClouderaManagerUIService();
        Stack stack = createStackWithComponents(new ExposedService[] { clouderaManagerUIService, exposedService("ATLAS") },
                new ExposedService[] { exposedService("HIVE_SERVER") }, GatewayType.INDIVIDUAL);
        stack.getCluster().getGateway().setGatewayPort(8443);

        String result = underTest.getManagerServerUrl(stack, CLOUDERA_MANAGER_IP);
        assertEquals("https://127.0.0.1:8443/gateway-path/topology1/cmf/home/", result);
    }

    @Test
    public void testPrepareClusterExposedServicesDisplayPropertiesOverride() {
        Stack stack = createStackWithComponents(new ExposedService[] { exposedService("HUE") },
                new ExposedService[] { exposedService("HUE") }, GatewayType.INDIVIDUAL);
        stack.getCluster().getGateway().setGatewayPort(443);

        mockBlueprintTextProcessor();
        mockComponentLocator(Lists.newArrayList("10.0.0.1"));

        when(exposedServiceCollector.knoxServicesForComponents(any(Optional.class), anyList())).thenReturn(List.of(exposedService("HUE"),
                exposedService("ANOTHER")));
        SupportedServices supportedServices = new SupportedServices();
        SupportedService supportedService = new SupportedService();
        supportedService.setIconKey("icon");
        supportedService.setDisplayName("display");
        supportedService.setName("HUE");
        supportedServices.setServices(Set.of(supportedService));
        when(templateGeneratorService.getServicesByBlueprint(any())).thenReturn(supportedServices);

        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesMap =
                underTest.prepareClusterExposedServices(stack, "10.0.0.1");

        Collection<ClusterExposedServiceV4Response> clusterExposedServiceV4Responses = clusterExposedServicesMap.get("topology1-api");
        assertEquals(2L, clusterExposedServiceV4Responses.size());
        ClusterExposedServiceV4Response hueSvcResponse = clusterExposedServiceV4Responses.stream()
                .filter(svcRsp -> svcRsp.getKnoxService().equals("HUE")).findFirst().orElseThrow();
        assertEquals("display", hueSvcResponse.getDisplayName());
        assertEquals("icon", hueSvcResponse.getIconKey());
    }

    @Test
    public void testPrepareClusterExposedServices() {
        Stack stack = createStackWithComponents(new ExposedService[] { exposedService("ATLAS") },
                new ExposedService[] { exposedService("HIVE_SERVER"), exposedService("WEBHDFS") }, GatewayType.INDIVIDUAL);
        stack.getCluster().getGateway().setGatewayPort(443);

        mockBlueprintTextProcessor();
        mockComponentLocator(Lists.newArrayList("10.0.0.1"));

        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesMap =
                underTest.prepareClusterExposedServices(stack, "10.0.0.1");

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
    public void testPrepareClusterExposedServicesByGeneratedBlueprint() {
        Stack stack = createStackWithComponents(new ExposedService[] { exposedService("ATLAS") },
                new ExposedService[] { exposedService("HIVE_SERVER"), exposedService("WEBHDFS") }, GatewayType.INDIVIDUAL);
        stack.getCluster().getGateway().setGatewayPort(443);
        stack.getCluster().setExtendedBlueprintText("extended-blueprint");

        mockBlueprintTextProcessor();
        mockComponentLocator(Lists.newArrayList("10.0.0.1"));

        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesMap =
                underTest.prepareClusterExposedServices(stack, "10.0.0.1");

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
    public void testPrepareClusterExposedServicesIfBlueprintNull() {
        Stack stack = createStackWithComponents(new ExposedService[] { exposedService("ATLAS") },
                new ExposedService[] { exposedService("HIVE_SERVER"), exposedService("WEBHDFS") }, GatewayType.INDIVIDUAL);
        stack.getCluster().getGateway().setGatewayPort(443);
        stack.getCluster().setExtendedBlueprintText("extended-blueprint");
        Blueprint blueprint = new Blueprint();
        blueprint.setStackVersion("7.2.14");
        stack.getCluster().setBlueprint(blueprint);
        mockBlueprintTextProcessor();
        mockComponentLocator(Lists.newArrayList("10.0.0.1"));

        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesMap =
                underTest.prepareClusterExposedServices(stack, "10.0.0.1");

        assertEquals(4L, clusterExposedServicesMap.keySet().size());
    }

    //If the private ip list is empty, cluster does not have any hostgroup.
    @Test
    public void testPrepareClusterExposedServicesIfPrivateIpsEmpty() {
        Stack stack = createStackWithComponents(new ExposedService[] { exposedService("ATLAS") },
                new ExposedService[] { exposedService("HIVE_SERVER"), exposedService("WEBHDFS") }, GatewayType.INDIVIDUAL);
        stack.getCluster().getGateway().setGatewayPort(443);
        stack.getCluster().setExtendedBlueprintText("extended-blueprint");
        mockBlueprintTextProcessor();
        when(componentLocatorService.getComponentLocationEvenIfStopped(any(), any(), any())).thenReturn(emptyMap());

        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesMap =
                underTest.prepareClusterExposedServices(stack, "10.0.0.1");

        assertEquals(4L, clusterExposedServicesMap.keySet().size());
    }

    @Test
    public void testPrepareClusterExposedServicesWhenImpalaDebugUi() {
        when(exposedServiceCollector.knoxServicesForComponents(any(Optional.class), anyList())).thenReturn(
                List.of(exposedService("CLOUDERA_MANAGER"), exposedService("CLOUDERA_MANAGER_UI"),
                        exposedService("NAMENODE"), exposedService("IMPALA_DEBUG_UI")));
        when(exposedServiceCollector.getImpalaDebugUIService()).thenReturn(
                exposedService("IMPALA_DEBUG_UI"));
        Stack stack = createStackWithComponents(new ExposedService[] { exposedService("IMPALA_DEBUG_UI") },
                new ExposedService[] { exposedService("HIVE_SERVER"), exposedService("IMPALA_DEBUG_UI") }, GatewayType.INDIVIDUAL);
        stack.getCluster().getGateway().setGatewayPort(443);
        stack.getCluster().setExtendedBlueprintText("blueprintOfTheYear");
        mockBlueprintTextProcessor();
        Map<String, List<String>> componentPrivateIps = Maps.newHashMap();
        componentPrivateIps.put("IMPALA_DEBUG_UI", List.of("10.0.0.1"));
        when(componentLocatorService.getComponentLocationEvenIfStopped(any(), any(), any())).thenReturn(componentPrivateIps);

        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesMap =
                underTest.prepareClusterExposedServices(stack, "10.0.0.1");

        assertEquals(4L, clusterExposedServicesMap.keySet().size());
        assertTrue(clusterExposedServicesMap.get("topology1-api").stream().anyMatch(exposed -> exposed.getServiceName().equals("IMPALA_DEBUG_UI")));
    }

    @Test
    public void testPrepareClusterExposedServicesWhenImpalaDebugWhenOnlyImpalaServiceAvailable() {
        when(exposedServiceCollector.knoxServicesForComponents(any(Optional.class), anyList())).thenReturn(
                List.of(exposedService("CLOUDERA_MANAGER"), exposedService("CLOUDERA_MANAGER_UI"),
                        exposedService("NAMENODE"), exposedService("IMPALA_DEBUG_UI")));
        when(exposedServiceCollector.getImpalaDebugUIService()).thenReturn(
                exposedService("IMPALA_DEBUG_UI"));
        Stack stack = createStackWithComponents(new ExposedService[] { exposedService("IMPALA") },
                new ExposedService[] { exposedService("HIVE_SERVER") }, GatewayType.INDIVIDUAL);
        stack.getCluster().getGateway().setGatewayPort(443);
        stack.getCluster().setExtendedBlueprintText("blueprintOfTheYear");
        mockBlueprintTextProcessor();
        Map<String, List<String>> componentPrivateIps = Maps.newHashMap();
        componentPrivateIps.put("IMPALA_DEBUG_UI", List.of("10.0.0.1"));
        componentPrivateIps.put("IMPALA", List.of("10.0.0.1"));

        when(componentLocatorService.getComponentLocationEvenIfStopped(any(), any(), any())).thenReturn(componentPrivateIps);

        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesMap =
                underTest.prepareClusterExposedServices(stack, "10.0.0.1");

        assertEquals(4L, clusterExposedServicesMap.keySet().size());
        assertTrue(clusterExposedServicesMap.get("topology1-api").stream().anyMatch(exposed -> exposed.getServiceName().equals("IMPALA_DEBUG_UI")));
    }

    @Test
    public void testGetKnoxServices() {
        mockBlueprintTextProcessor();
        Collection<ExposedServiceV4Response> exposedServiceResponses = underTest.getKnoxServices(workspace.getId(), "blueprint");
        assertEquals(4L, exposedServiceResponses.size());

        mockBlueprintTextProcessor();

        exposedServiceResponses = underTest.getKnoxServices(workspace.getId(), "blueprint");
        assertEquals(4L, exposedServiceResponses.size());

        mockBlueprintTextProcessor();

        exposedServiceResponses = underTest.getKnoxServices(workspace.getId(), "blueprint");
        assertEquals(4L, exposedServiceResponses.size());
    }

    @Test
    public void testGetKnoxServicesWithLivyServer() {
        mockBlueprintTextProcessor();

        Collection<ExposedServiceV4Response> exposedServiceResponses = underTest.getKnoxServices(workspace.getId(), "blueprint");
        assertEquals(4L, exposedServiceResponses.size());

        mockBlueprintTextProcessor();

        exposedServiceResponses = underTest.getKnoxServices(workspace.getId(), "blueprint");
        assertEquals(4L, exposedServiceResponses.size());
    }

    private void mockBlueprintTextProcessor() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("{\"Blueprints\":{}}");
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        blueprint.setWorkspace(workspace);
        lenient().when(blueprintService.getByNameForWorkspaceId(any(), anyLong())).thenReturn(blueprint);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(any())).thenReturn(cmTemplateProcessor);
    }

    private void mockComponentLocator(List<String> privateIps) {
        Map<String, List<String>> componentPrivateIps = Maps.newHashMap();
        componentPrivateIps.put("NAMENODE", privateIps);
        when(componentLocatorService.getComponentLocationEvenIfStopped(any(), any(), any())).thenReturn(componentPrivateIps);
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

    private Stack stackWithOrchestrator(String orchestratorType) {
        Cluster cluster = new Cluster();
        Stack stack = new Stack();
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType(orchestratorType);
        Gateway gateway = new Gateway();
        gateway.setPath(GATEWAY_PATH);
        stack.setOrchestrator(orchestrator);
        stack.setCluster(cluster);
        cluster.setStack(stack);
        cluster.setGateway(gateway);
        Blueprint blueprint = new Blueprint();
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        cluster.setWorkspace(workspace);
        stack.setWorkspace(workspace);
        try {
            String testBlueprint = FileReaderUtils.readFileFromClasspath("/test/defaults/blueprints/hdp26-data-science-spark2-text.bp");
            blueprint.setBlueprintText(testBlueprint);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cluster.setBlueprint(blueprint);
        return stack;
    }

    private Stack createStackWithComponents(ExposedService[] topology1Services, ExposedService[] topology2Services, GatewayType gatewayType) {
        Stack stack = stackWithOrchestrator("ANY");
        Cluster cluster = stack.getCluster();
        GatewayTopology topology1 = gatewayTopology("topology1", topology1Services);
        topology1.setGateway(cluster.getGateway());
        GatewayTopology topology2 = gatewayTopology("topology2", topology2Services);
        topology2.setGateway(cluster.getGateway());
        cluster.getGateway().setTopologies(Sets.newHashSet(topology1, topology2));
        cluster.getGateway().setGatewayType(gatewayType);
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        cluster.setWorkspace(workspace);
        stack.setType(StackType.WORKLOAD);
        return stack;
    }

    private ExposedService getClouderaManagerUIService() {
        ExposedService clouderaManagerUIService = exposedService("CLOUDERA_MANAGER_UI");
        clouderaManagerUIService.setKnoxUrl("/cmf/home/");
        clouderaManagerUIService.setPort(443);
        return clouderaManagerUIService;
    }
}
