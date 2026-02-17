package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;
import com.sequenceiq.cloudbreak.cmtemplate.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToExposedServicesConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

/**
 * ServiceEndpointCollector tests for services deployed in operational database clusters
 */
@ExtendWith(MockitoExtension.class)
public class OpdbServiceEndpointCollectorTest {

    private static final String GATEWAY_PATH = "gateway-path";

    private static final MultiValuedMap<String, ExposedService> EXPOSED_SERVICES = parseExposedServices();

    @Mock
    private BlueprintService blueprintService;

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

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ExposedServiceCollector exposedServiceCollector;

    @Mock
    private ServiceEndpointCollectorEntitlementComparator serviceEndpointCollectorEntitlementComparator;

    @Mock
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Mock
    private CmTemplateGeneratorService templateGeneratorService;

    @InjectMocks
    private final GatewayTopologyV4RequestToExposedServicesConverter exposedServicesConverter =
            new GatewayTopologyV4RequestToExposedServicesConverter();

    private static MultiValuedMap<String, ExposedService> parseExposedServices() {
        MultiValuedMap<String, ExposedService> services = new ArrayListValuedHashMap<>();

        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("definitions/exposed-services.json")) {
            String rawJson = IOUtils.toString(is, StandardCharsets.UTF_8);
            JsonUtil.readValue(rawJson, com.sequenceiq.cloudbreak.api.service.ExposedServices.class)
                    .getServices()
                    .forEach(exposedService -> services.put(exposedService.getName(), exposedService));

            return services;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the ExposedService object from the parsed exposed-services.json file, or fails if no such
     * element exists with the same name.
     */
    ExposedService getExposedServiceOrFail(String name) {
        return EXPOSED_SERVICES.get(name)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No such exposed service with name: " + name));
    }

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(underTest, "httpsPort", "443");
        // Called implicitly in ServiceEndpointCollector
        when(exposedServiceCollector.getClouderaManagerUIService()).thenReturn(getExposedServiceOrFail("CLOUDERA_MANAGER_UI"));
        when(exposedServiceCollector.getImpalaService()).thenReturn(getExposedServiceOrFail("IMPALA"));
        when(exposedServiceCollector.getHBaseUIService()).thenReturn(getExposedServiceOrFail("HBASE_UI"));
        when(exposedServiceCollector.getHBaseJarsService()).thenReturn(getExposedServiceOrFail("HBASEJARS"));
        Set<String> services = new HashSet<>();
        services.add("HBASEUI");
        services.add("HBASEJARS");
        services.add("WEBHBASE");
        services.add("AVATICA");
        services.add("CM-UI");
        services.add("CM-API");
        when(exposedServiceCollector.getFullServiceListBasedOnList(any(), any())).thenReturn(services);
        lenient().when(entitlementService.getEntitlements(anyString())).thenReturn(new ArrayList<>());
        when(serviceEndpointCollectorEntitlementComparator.entitlementSupported(anyList(), eq(null))).thenReturn(true);
        // Skip exposed service validation
        when(exposedServiceListValidator.validate(any())).thenReturn(ValidationResult.builder().build());
        // Couldn't get all mocks wired up with the Spring autowiring working. So, this is a pared down
        // version of what ExposedServiceCollector#knoxServicesForComponents actually does. The improvement
        // over what ServiceEndpointCollectorTest does is that this actually reads the real exposed-service.json
        // file to populate the data that the test uses.
        when(exposedServiceCollector.knoxServicesForComponents(any(Optional.class), any())).thenAnswer(invocation -> {
            Collection<String> components = invocation.getArgument(1);
            Collection<ExposedService> services1 = filterSupportedKnoxServices();
            return services1.stream().filter(exposedService -> components.contains(exposedService.getServiceName()))
                    .collect(Collectors.toList());
        });
        when(clouderaManagerProductsProvider.findCdhProduct(anySet())).thenReturn(Optional.of(new ClouderaManagerProduct().withVersion("7.3.1-1.cdh7.3.1.p0")));
        when(templateGeneratorService.getServicesByBlueprint(any())).thenReturn(new SupportedServices());
    }

    Collection<ExposedService> filterSupportedKnoxServices() {
        return EXPOSED_SERVICES.values();
    }

    private void mockTemplateComponents() {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(any())).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.getAllComponents()).thenReturn(new HashSet<>(Arrays.asList(
                ServiceComponent.of("HBASE", "MASTER"),
                ServiceComponent.of("HBASE", "REGIONSERVER"),
                ServiceComponent.of("HBASE", "HBASERESTSERVER"),
                ServiceComponent.of("PHOENIX", "PHOENIX_QUERY_SERVER"),
                ServiceComponent.of("CLOUDERA_MANAGER", "CM-API"),
                ServiceComponent.of("CLOUDERA_MANAGER_UI", "CM-UI")
        )));
    }

    private void mockComponentLocator(List<String> privateIps) {
        Map<String, List<String>> componentPrivateIps = Maps.newHashMap();
        componentPrivateIps.put("MASTER", privateIps);
        componentPrivateIps.put("REGIONSERVER", privateIps);
        componentPrivateIps.put("HBASERESTSERVER", privateIps);
        componentPrivateIps.put("PHOENIX_QUERY_SERVER", privateIps);
        componentPrivateIps.put("CM-UI", privateIps);
        componentPrivateIps.put("CM-API", privateIps);
        when(componentLocatorService.getComponentLocationEvenIfStopped(any(), any(), any())).thenReturn(componentPrivateIps);
    }

    @Test
    public void testPrepareClusterExposedServices() {
        StackDto stack = createStackDtoWithComponents(GatewayType.INDIVIDUAL, 443);

        mockTemplateComponents();
        mockComponentLocator(Lists.newArrayList("10.0.0.1"));

        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesMap =
                underTest.prepareClusterExposedServices(stack, "10.0.0.1");

        assertEquals(2L, clusterExposedServicesMap.keySet().size(), clusterExposedServicesMap.toString());

        Collection<ClusterExposedServiceV4Response> proxyServices = clusterExposedServicesMap.get("proxy");
        Collection<ClusterExposedServiceV4Response> proxyApiServices = clusterExposedServicesMap.get("proxy-api");

        assertNotNull(proxyServices, "Topology proxy services was null");
        assertNotNull(proxyApiServices, "Topology proxy API services was null");

        Set<String> proxyServiceNames = proxyServices.stream()
                .map(ClusterExposedServiceV4Response::getKnoxService).collect(Collectors.toSet());
        Set<String> proxyApiServiceNames = proxyApiServices.stream()
                .map(ClusterExposedServiceV4Response::getKnoxService).collect(Collectors.toSet());

        assertEquals(2, proxyServiceNames.size(), proxyServiceNames.toString());
        assertEquals(4, proxyApiServiceNames.size(), proxyApiServiceNames.toString());
        assertEquals(new HashSet<>(Arrays.asList("CM-UI", "HBASEUI")), proxyServiceNames);
        assertEquals(new HashSet<>(Arrays.asList("CM-API", "HBASEJARS", "WEBHBASE", "AVATICA")), proxyApiServiceNames);
        Optional<ClusterExposedServiceV4Response> hbasejars = proxyApiServices.stream().filter(
                service -> service.getKnoxService().equals("HBASEJARS")).findFirst();
        Optional<ClusterExposedServiceV4Response> avatica = proxyApiServices.stream().filter(
                service -> service.getKnoxService().equals("AVATICA")).findFirst();
        assertTrue(hbasejars.isPresent());
        assertTrue(avatica.isPresent());
        assertEquals("https://10.0.0.1/gateway-path/proxy-api/hbase/jars", hbasejars.get().getServiceUrl());
        assertEquals("https://10.0.0.1/gateway-path/proxy-api/avatica/", avatica.get().getServiceUrl());
    }

    private GatewayTopology gatewayTopology(String name) {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(name);
        gatewayTopologyJson.setExposedServices(Arrays.asList("HBASEUI", "HBASEJARS", "AVATICA", "CM-UI", "CM-API",
                "WEBHBASE"));
        ExposedServices exposedServices = exposedServicesConverter.convert(gatewayTopologyJson);
        GatewayTopology gatewayTopology = new GatewayTopology();
        gatewayTopology.setTopologyName(name);
        gatewayTopology.setExposedServices(new Json(exposedServices));
        return gatewayTopology;
    }

    private Cluster clusterWithOrchestrator(String orchestratorType) {
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
            String testBlueprint = FileReaderUtils.readFileFromClasspath("/defaults/blueprints/7.2.10/cdp-opdb.bp");
            blueprint.setBlueprintText(testBlueprint);
            cluster.setExtendedBlueprintText(testBlueprint);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cluster.setBlueprint(blueprint);
        return cluster;
    }

    private StackDto createStackDtoWithComponents(GatewayType gatewayType, int gatewayPort) {
        Cluster cluster = clusterWithOrchestrator("ANY");
        GatewayTopology topology1 = gatewayTopology("proxy");
        topology1.setGateway(cluster.getGateway());
        cluster.getGateway().setTopologies(Collections.singleton(topology1));
        cluster.getGateway().setGatewayType(gatewayType);
        cluster.getGateway().setGatewayPort(gatewayPort);
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        cluster.setWorkspace(workspace);
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDto.getGateway()).thenReturn(cluster.getGateway());
        when(stackDto.getBlueprint()).thenReturn(cluster.getBlueprint());
        when(stackDto.getBlueprintJsonText()).thenReturn(cluster.getBlueprint().getBlueprintJsonText());
        when(stackDto.getOrchestrator()).thenReturn(cluster.getStack().getOrchestrator());
        when(stackDto.getType()).thenReturn(StackType.WORKLOAD);
        return stackDto;
    }
}
