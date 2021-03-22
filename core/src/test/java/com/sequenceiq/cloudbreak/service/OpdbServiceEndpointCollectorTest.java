package com.sequenceiq.cloudbreak.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
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
@RunWith(MockitoJUnitRunner.class)
public class OpdbServiceEndpointCollectorTest {

    private static final String GATEWAY_PATH = "gateway-path";

    private static final Map<String, ExposedService> EXPOSED_SERVICES = parseExposedServices();

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
    private ServiceEndpointCollectorVersionComparator serviceEndpointCollectorVersionComparator;

    @Mock
    private ServiceEndpointCollectorEntitlementComparator serviceEndpointCollectorEntitlementComparator;

    @InjectMocks
    private final GatewayTopologyV4RequestToExposedServicesConverter exposedServicesConverter =
    new GatewayTopologyV4RequestToExposedServicesConverter();

    @Mock
    private Workspace workspace;

    private static Map<String, ExposedService> parseExposedServices() {
        String rawJson;
        try (BufferedReader bs = new BufferedReader(
                new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(
                    "definitions/exposed-services.json")))) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = bs.readLine()) != null) {
                out.append(line);
            }
            rawJson = out.toString();

            return JsonUtil.readValue(rawJson, com.sequenceiq.cloudbreak.api.service.ExposedServices.class)
                .getServices()
                .stream()
                .collect(Collectors.toMap(ExposedService::getName, Function.identity()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the ExposedService object from the parsed exposed-services.json file, or fails if no such
     * element exists with the same name.
     */
    ExposedService getExposedServiceOrFail(String name) {
        ExposedService svc = EXPOSED_SERVICES.get(name);
        if (svc == null) {
            throw new NoSuchElementException("No such exposed service with name: " + name);
        }
        return svc;
    }

    @Before
    public void setup() {
        ReflectionTestUtils.setField(underTest, "httpsPort", "443");
        // Called implicitly in ServiceEndpointCollector
        when(exposedServiceCollector.getClouderaManagerUIService()).thenReturn(
            getExposedServiceOrFail("CLOUDERA_MANAGER_UI"));
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
        when(exposedServiceCollector.getFullServiceListBasedOnList(any())).thenReturn(services);
        when(serviceEndpointCollectorVersionComparator.maxVersionSupported(any(), any())).thenReturn(true);
        when(serviceEndpointCollectorVersionComparator.minVersionSupported(any(), any())).thenReturn(true);
        when(entitlementService.getEntitlements(anyString())).thenReturn(new ArrayList<>());
        when(serviceEndpointCollectorEntitlementComparator.entitlementSupported(anyList(), eq(null))).thenReturn(true);
        // Skip exposed service validation
        when(exposedServiceListValidator.validate(any())).thenReturn(ValidationResult.builder().build());
        // Couldn't get all mocks wired up with the Spring autowiring working. So, this is a pared down
        // version of what ExposedServiceCollector#knoxServicesForComponents actually does. The improvement
        // over what ServiceEndpointCollectorTest does is that this actually reads the real exposed-service.json
        // file to populate the data that the test uses.
        when(exposedServiceCollector.knoxServicesForComponents(any())).thenAnswer(new Answer<>() {
            @Override
            public Collection<ExposedService> answer(InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("unchecked")
                Collection<String> components = (Collection<String>) invocation.getArgument(0);
                Collection<ExposedService> services = filterSupportedKnoxServices();
                return services.stream().filter(exposedService -> components.contains(exposedService.getServiceName()))
                    .collect(Collectors.toList());
            }
        });
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
        when(componentLocatorService.getComponentLocation(any(), any(), any())).thenReturn(componentPrivateIps);
    }

    @Test
    public void testPrepareClusterExposedServices() {
        Cluster cluster = createClusterWithComponents(GatewayType.INDIVIDUAL);
        cluster.getGateway().setGatewayPort(443);

        mockTemplateComponents();
        mockComponentLocator(Lists.newArrayList("10.0.0.1"));

        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesMap =
                underTest.prepareClusterExposedServices(cluster, "10.0.0.1");

        assertEquals(clusterExposedServicesMap.toString(), 2L, clusterExposedServicesMap.keySet().size());

        Collection<ClusterExposedServiceV4Response> proxyServices = clusterExposedServicesMap.get("proxy");
        Collection<ClusterExposedServiceV4Response> proxyApiServices = clusterExposedServicesMap.get("proxy-api");

        assertNotNull("Topology proxy services was null", proxyServices);
        assertNotNull("Topology proxy API services was null", proxyApiServices);

        Set<String> proxyServiceNames = proxyServices.stream()
            .map(ClusterExposedServiceV4Response::getKnoxService).collect(Collectors.toSet());
        Set<String> proxyApiServiceNames = proxyApiServices.stream()
            .map(ClusterExposedServiceV4Response::getKnoxService).collect(Collectors.toSet());

        assertEquals(proxyServiceNames.toString(), 2, proxyServiceNames.size());
        assertEquals(proxyApiServiceNames.toString(), 4, proxyApiServiceNames.size());
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
            String testBlueprint = FileReaderUtils.readFileFromClasspath("/defaults/blueprints/7.2.0/cdp-opdb.bp");
            blueprint.setBlueprintText(testBlueprint);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        cluster.setBlueprint(blueprint);
        return cluster;
    }

    private Cluster createClusterWithComponents(GatewayType gatewayType) {
        Cluster cluster = clusterWithOrchestrator("ANY");
        GatewayTopology topology1 = gatewayTopology("proxy");
        topology1.setGateway(cluster.getGateway());
        cluster.getGateway().setTopologies(Collections.singleton(topology1));
        cluster.getGateway().setGatewayType(gatewayType);
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        cluster.setWorkspace(workspace);
        return cluster;
    }
}
