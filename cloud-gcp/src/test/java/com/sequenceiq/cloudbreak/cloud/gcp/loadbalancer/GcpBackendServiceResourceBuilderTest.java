package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpBackendServiceResourceBuilder.GCP_INSTANCEGROUP_REFERENCE_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.CollectionUtils;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.RegionBackendServices;
import com.google.api.services.compute.model.Backend;
import com.google.api.services.compute.model.BackendService;
import com.google.api.services.compute.model.InstanceGroupsListInstances;
import com.google.api.services.compute.model.InstanceGroupsListInstancesRequest;
import com.google.api.services.compute.model.InstanceWithNamedPorts;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpBackendServiceResourceBuilderTest {

    private static final String AVAILABILITY_ZONE = "us-west2-c";

    private static final String AVAILABILITY_ZONE_B = "us-west2-b";

    private static final String REGION = "us-west2";

    private static final String PROJECT_ID = "projectId";

    @Mock
    private GcpContext gcpContext;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudLoadBalancer cloudLoadBalancer;

    @InjectMocks
    private GcpBackendServiceResourceBuilder underTest;

    @Mock
    private Compute compute;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private AvailabilityZone availabilityZone;

    @Mock
    private RegionBackendServices regionBackendServices;

    @Mock
    private Group group;

    @Mock
    private GroupNetwork groupNetwork;

    @Mock
    private Operation operation;

    @Mock
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    @Mock
    private Compute.InstanceGroups instanceGroups;

    @Mock
    private ResourceRetriever resourceRetriever;

    private CloudStack cloudStack;

    static Object[][] dataForInstanceGroups() {
        return new Object[][]{
                {Map.of("us-west2-a", true, "us-west2-b", true, "us-west2-c", true)},
                {Map.of("us-west2-a", false, "us-west2-b", false, "us-west2-c", false)},
                {Map.of("us-west2-a", true, "us-west2-b", false, "us-west2-c", true)},
                {Map.of("us-west2-a", true, "us-west2-b", false)},
                {Map.of()}
        };
    }

    @BeforeEach
    void setup() {
        Map<InstanceGroupType, String> userData = ImmutableMap.of(InstanceGroupType.CORE, "CORE", InstanceGroupType.GATEWAY, "GATEWAY");
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "", "default", "default-id", new HashMap<>(), "2019-10-24",
                1571884856L, null);
        GcpResourceNameService resourceNameService = new GcpResourceNameService();
        ReflectionTestUtils.setField(resourceNameService, "maxResourceNameLength", 50);
        ReflectionTestUtils.setField(underTest, "resourceNameService", resourceNameService);
        ReflectionTestUtils.setField(underTest, "resourceRetriever", resourceRetriever);
        Network network = new Network(null);
        cloudStack = CloudStack.builder()
                .network(network)
                .image(image)
                .build();
    }

    @Test
    void testCreateWhereEverythingGoesFine() {
        when(gcpContext.getName()).thenReturn("name");
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(80, 8080), Collections.emptySet());
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);

        List<CloudResource> cloudResources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, cloudStack.getNetwork());

        assertTrue(cloudResources.get(0).getName().startsWith("name-public-8080"));
        assertEquals(1, cloudResources.size());
        assertEquals(8080, cloudResources.get(0).getParameter("hcport", HealthProbeParameters.class).getPort());
        List<TargetGroupPortPair> traffics = cloudResources.get(0).getParameter("trafficports", List.class);
        assertEquals(80, traffics.get(0).getTrafficPort());
    }

    @Test
    void testCreateWithPrivateMultipleProtocolsAndPorts() {
        when(gcpContext.getName()).thenReturn("name");
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);
        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        HealthProbeParameters tcpHealth = new HealthProbeParameters("path", 8080, NetworkProtocol.TCP, 0, 0);
        HealthProbeParameters httpsHealth = new HealthProbeParameters("path", 8081, NetworkProtocol.HTTPS, 0, 0);
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(80, NetworkProtocol.TCP, tcpHealth), Collections.emptySet());
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(81, NetworkProtocol.UDP, tcpHealth), Collections.emptySet());
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(82, NetworkProtocol.TCP, httpsHealth), Collections.emptySet());
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(83, NetworkProtocol.UDP, httpsHealth), Collections.emptySet());
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);

        List<CloudResource> cloudResources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, cloudStack.getNetwork());

        List<CloudResource> hc8080Resources = cloudResources.stream()
                .filter(cloudResource -> cloudResource.getParameter("hcport", HealthProbeParameters.class).getPort() == 8080)
                .toList();
        assertEquals(1, hc8080Resources.size());
        assertTrue(hc8080Resources.get(0).getName().startsWith("name-private-8080"));
        List<TargetGroupPortPair> traffics8080 = hc8080Resources.get(0).getParameter("trafficports", List.class);
        assertTrue(traffics8080.contains(new TargetGroupPortPair(80, NetworkProtocol.TCP, tcpHealth)));
        assertTrue(traffics8080.contains(new TargetGroupPortPair(81, NetworkProtocol.UDP, tcpHealth)));

        List<CloudResource> hc8081Resources = cloudResources.stream()
                .filter(cloudResource -> cloudResource.getParameter("hcport", HealthProbeParameters.class).getPort() == 8081)
                .toList();
        assertEquals(1, hc8081Resources.size());
        assertTrue(hc8081Resources.get(0).getName().startsWith("name-private-8081"));
        List<TargetGroupPortPair> traffics8081 = hc8081Resources.get(0).getParameter("trafficports", List.class);
        assertTrue(traffics8081.contains(new TargetGroupPortPair(82, NetworkProtocol.TCP, httpsHealth)));
        assertTrue(traffics8081.contains(new TargetGroupPortPair(83, NetworkProtocol.UDP, httpsHealth)));
    }

    @Test
    void testCreateWithPrivateLoadBalancerAndExistingResources() {
        when(gcpContext.getName()).thenReturn("name");
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);

        HealthProbeParameters healthProbe1 = new HealthProbeParameters("path", 8080, NetworkProtocol.TCP, 0, 0);
        HealthProbeParameters healthProbe2 = new HealthProbeParameters("path", 8081, NetworkProtocol.HTTPS, 0, 0);

        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(80, NetworkProtocol.TCP, healthProbe1), Collections.emptySet());
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(82, NetworkProtocol.TCP, healthProbe2), Collections.emptySet());
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);

        CloudResource existingPublicResource = CloudResource.builder()
                .withType(ResourceType.GCP_BACKEND_SERVICE)
                .withStatus(CommonStatus.CREATED)
                .withName("public-resource")
                .withParameters(Map.of(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.PUBLIC.asMap()))
                .build();

        CloudResource existingPrivateResource = CloudResource.builder()
                .withType(ResourceType.GCP_BACKEND_SERVICE)
                .withStatus(CommonStatus.CREATED)
                .withName("private-" + healthProbe1.getPort() + "-resource")
                .withParameters(Map.of(
                        "hcport", healthProbe1,
                        CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.PRIVATE.asMap())
                )
                .build();

        when(resourceRetriever.findAllByStatusAndTypeAndStack(any(), any(), any()))
                .thenReturn(new ArrayList<>(List.of(existingPublicResource, existingPrivateResource)));

        List<CloudResource> cloudResources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, cloudStack.getNetwork());

        assertEquals(2, cloudResources.size());
        CloudResource newResource = cloudResources.stream().filter(resource -> resource.getName().contains("8081")).findFirst().get();
        assertTrue(newResource.getName().startsWith("name-private-8081"));
        assertEquals(healthProbe2, newResource.getParameter("hcport", HealthProbeParameters.class));
        CloudResource existingResource = cloudResources.stream().filter(resource -> resource.getName().contains("8080")).findFirst().get();
        assertEquals(existingPrivateResource.getName(), existingResource.getName());
    }

    @ParameterizedTest(name = "testBuildWithSeparateHCPort{index}")
    @MethodSource("dataForInstanceGroups")
    void testBuildWithSeparateHCPort(Map<String, Boolean> instanceGroupsMap) throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", new HealthProbeParameters(null, 8080, null, 0, 0));
        parameters.put("trafficports", List.of(new TargetGroupPortPair(80, 8080)));

        CloudResource hcResource = CloudResource.builder()
                .withType(ResourceType.GCP_HEALTH_CHECK)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("hcsuper")
                .withParameters(parameters)
                .withPersistent(true)
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();

        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_BACKEND_SERVICE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withParameters(parameters)
                .withPersistent(true)
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();

        when(group.getName()).thenReturn("master");
        when(group.getNetwork()).thenReturn(groupNetwork);
        when(groupNetwork.getAvailabilityZones()).thenReturn(instanceGroupsMap.keySet());
        if (CollectionUtils.isEmpty(instanceGroupsMap.keySet())) {
            instanceGroupsMap = Map.of(AVAILABILITY_ZONE, true);
        }

        Compute.RegionBackendServices.Insert insert = mock(Compute.RegionBackendServices.Insert.class);

        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(80, 8080), Set.of(group));
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getName()).thenReturn("stackName");
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
        when(gcpContext.getLoadBalancerResources(any())).thenReturn(List.of(hcResource));
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(availabilityZone.value()).thenReturn(AVAILABILITY_ZONE);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn(REGION);

        when(compute.regionBackendServices()).thenReturn(regionBackendServices);

        for (Map.Entry<String, Boolean> entry : instanceGroupsMap.entrySet()) {
            InstanceGroupsListInstances instanceGroupsListInstances = mock(InstanceGroupsListInstances.class);
            InstanceWithNamedPorts instanceWithNamedPorts = mock(InstanceWithNamedPorts.class);
            when(instanceGroupsListInstances.getItems()).thenReturn(entry.getValue() ? List.of(instanceWithNamedPorts) : List.of());
            Compute.InstanceGroups.ListInstances listInstances = mock(Compute.InstanceGroups.ListInstances.class);
            when(listInstances.execute()).thenReturn(instanceGroupsListInstances);
            when(instanceGroups.listInstances(PROJECT_ID, entry.getKey(), groupName("stackname-master-111", entry.getKey()),
                    new InstanceGroupsListInstancesRequest())).thenReturn(listInstances);
        }

        when(compute.instanceGroups()).thenReturn(instanceGroups);
        ArgumentCaptor<BackendService> backendServiceArgumentCaptor = ArgumentCaptor.forClass(BackendService.class);
        Compute.RegionBackendServices.Get getRequest = mock(Compute.RegionBackendServices.Get.class);
        when(getRequest.execute()).thenReturn(null);
        when(regionBackendServices.get(PROJECT_ID, REGION, resource.getName())).thenReturn(getRequest);
        when(regionBackendServices.insert(eq(PROJECT_ID), eq(REGION), backendServiceArgumentCaptor.capture())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);
        when(gcpLoadBalancerTypeConverter.getScheme(any(CloudLoadBalancer.class))).thenCallRealMethod();
        when(authenticatedContext.getCloudContext().getId()).thenReturn(111L);

        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        assertEquals("super", cloudResources.get(0).getName());

        assertEquals(8080, cloudResources.get(0).getParameter("hcport", HealthProbeParameters.class).getPort());
        List<TargetGroupPortPair> traffics = cloudResources.get(0).getParameter("trafficports", List.class);
        assertEquals(80, traffics.get(0).getTrafficPort());
        BackendService backendService = backendServiceArgumentCaptor.getValue();
        assertEquals(instanceGroupsMap.values().stream().filter(Boolean::booleanValue).count(), backendService.getBackends().size());
        for (Map.Entry<String, Boolean> entry : instanceGroupsMap.entrySet()) {
            assertEquals(entry.getValue(), backendService.getBackends().stream().anyMatch(
                    backend -> backend.getGroup().equals(String.format(GCP_INSTANCEGROUP_REFERENCE_FORMAT,
                            PROJECT_ID, entry.getKey(), groupName("stackname-master-111", entry.getKey())))));

        }
        assertEquals("TCP", backendService.getProtocol());
    }

    @Test
    void testBuildWithMultipleTrafficProtocolsAndPorts() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        HealthProbeParameters healthProbe = new HealthProbeParameters(null, 8443, null, 0, 0);
        parameters.put("hcport", healthProbe);
        parameters.put("trafficports", List.of(new TargetGroupPortPair(443, NetworkProtocol.TCP, healthProbe),
                new TargetGroupPortPair(11443, NetworkProtocol.UDP, healthProbe)));

        CloudResource hcResource = CloudResource.builder()
                .withType(ResourceType.GCP_HEALTH_CHECK)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("hcsuper")
                .withParameters(parameters)
                .withPersistent(true)
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();

        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_BACKEND_SERVICE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withParameters(parameters)
                .withPersistent(true)
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();

        when(group.getName()).thenReturn("master");
        when(group.getNetwork()).thenReturn(groupNetwork);

        Compute.RegionBackendServices.Insert insert = mock(Compute.RegionBackendServices.Insert.class);

        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(443, NetworkProtocol.TCP, new HealthProbeParameters(null, 8443, null, 0, 0)), Set.of(group));
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(11443, NetworkProtocol.UDP, new HealthProbeParameters(null, 8443, null, 0, 0)), Set.of(group));
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getName()).thenReturn("stackName");
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
        when(gcpContext.getLoadBalancerResources(any())).thenReturn(List.of(hcResource));
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(availabilityZone.value()).thenReturn(AVAILABILITY_ZONE);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn(REGION);

        when(compute.regionBackendServices()).thenReturn(regionBackendServices);
        InstanceGroupsListInstances instanceGroupsListInstances = mock(InstanceGroupsListInstances.class);
        Compute.InstanceGroups.ListInstances listInstances = mock(Compute.InstanceGroups.ListInstances.class);
        when(instanceGroupsListInstances.getItems()).thenReturn(List.of());
        when(listInstances.execute()).thenReturn(instanceGroupsListInstances);
        when(instanceGroups.listInstances(PROJECT_ID, AVAILABILITY_ZONE, groupName("stackname-master-111", AVAILABILITY_ZONE),
                new InstanceGroupsListInstancesRequest())).thenReturn(listInstances);
        when(compute.instanceGroups()).thenReturn(instanceGroups);
        ArgumentCaptor<BackendService> backendServiceArgumentCaptor = ArgumentCaptor.forClass(BackendService.class);
        Compute.RegionBackendServices.Get getRequest = mock(Compute.RegionBackendServices.Get.class);
        when(getRequest.execute()).thenReturn(null);
        when(regionBackendServices.get(PROJECT_ID, REGION, resource.getName())).thenReturn(getRequest);
        when(regionBackendServices.insert(anyString(), anyString(), backendServiceArgumentCaptor.capture())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);
        when(gcpLoadBalancerTypeConverter.getScheme(any(CloudLoadBalancer.class))).thenCallRealMethod();
        when(authenticatedContext.getCloudContext().getId()).thenReturn(111L);

        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        assertEquals("super", cloudResources.get(0).getName());

        assertEquals(8443, cloudResources.get(0).getParameter("hcport", HealthProbeParameters.class).getPort());
        List<TargetGroupPortPair> traffics = ((List<TargetGroupPortPair>) cloudResources.get(0).getParameters().get("trafficports"));
        assertTrue(traffics.contains(new TargetGroupPortPair(443, NetworkProtocol.TCP, healthProbe)));
        assertTrue(traffics.contains(new TargetGroupPortPair(11443, NetworkProtocol.UDP, healthProbe)));
        BackendService backendService = backendServiceArgumentCaptor.getValue();
        assertEquals("UNSPECIFIED", backendService.getProtocol());
    }

    @Test
    void testBuildWithMultipleTrafficPorts() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", new HealthProbeParameters(null, 8443, null, 0, 0));
        parameters.put("trafficports", List.of(new TargetGroupPortPair(443, 8443), new TargetGroupPortPair(11443, 8443)));

        CloudResource hcResource = CloudResource.builder()
                .withType(ResourceType.GCP_HEALTH_CHECK)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("hcsuper")
                .withParameters(parameters)
                .withPersistent(true)
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();

        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_BACKEND_SERVICE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withParameters(parameters)
                .withPersistent(true)
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();

        when(group.getName()).thenReturn("master");
        when(group.getNetwork()).thenReturn(groupNetwork);

        Compute.RegionBackendServices.Insert insert = mock(Compute.RegionBackendServices.Insert.class);

        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(443, 8443), Set.of(group));
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(11443, 8443), Set.of(group));
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getName()).thenReturn("stackName");
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
        when(gcpContext.getLoadBalancerResources(any())).thenReturn(List.of(hcResource));
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(availabilityZone.value()).thenReturn(AVAILABILITY_ZONE);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn(REGION);

        when(compute.regionBackendServices()).thenReturn(regionBackendServices);
        InstanceGroupsListInstances instanceGroupsListInstances = mock(InstanceGroupsListInstances.class);
        Compute.InstanceGroups.ListInstances listInstances = mock(Compute.InstanceGroups.ListInstances.class);
        when(instanceGroupsListInstances.getItems()).thenReturn(List.of());
        when(listInstances.execute()).thenReturn(instanceGroupsListInstances);
        when(instanceGroups.listInstances(PROJECT_ID, AVAILABILITY_ZONE, groupName("stackname-master-111", AVAILABILITY_ZONE),
                new InstanceGroupsListInstancesRequest())).thenReturn(listInstances);
        when(compute.instanceGroups()).thenReturn(instanceGroups);
        Compute.RegionBackendServices.Get getRequest = mock(Compute.RegionBackendServices.Get.class);
        when(getRequest.execute()).thenReturn(null);
        when(regionBackendServices.get(PROJECT_ID, REGION, resource.getName())).thenReturn(getRequest);
        when(regionBackendServices.insert(anyString(), anyString(), any())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);
        when(gcpLoadBalancerTypeConverter.getScheme(any(CloudLoadBalancer.class))).thenCallRealMethod();
        when(authenticatedContext.getCloudContext().getId()).thenReturn(111L);

        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        assertEquals("super", cloudResources.get(0).getName());

        assertEquals(8443, cloudResources.get(0).getParameter("hcport", HealthProbeParameters.class).getPort());
        List<Integer> ports = ((List<TargetGroupPortPair>) cloudResources.get(0).getParameters().get("trafficports")).stream()
                .map(TargetGroupPortPair::getTrafficPort)
                .toList();
        assertTrue(ports.contains(443));
        assertTrue(ports.contains(11443));
    }

    @Test
    void testDelete() throws Exception {
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_BACKEND_SERVICE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withParameters(new HashMap<>())
                .withPersistent(true)
                .build();

        Compute.RegionBackendServices.Delete delete = mock(Compute.RegionBackendServices.Delete.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn("us-west2");

        when(compute.regionBackendServices()).thenReturn(regionBackendServices);
        when(regionBackendServices.delete(anyString(), anyString(), any())).thenReturn(delete);
        when(delete.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");

        CloudResource cloudResource = underTest.delete(gcpContext, authenticatedContext, resource);

        assertEquals("super", cloudResource.getName());

        assertEquals(ResourceType.GCP_BACKEND_SERVICE, cloudResource.getType());
        assertEquals(CommonStatus.CREATED, cloudResource.getStatus());
    }

    private String groupName(String prefix, String availabilityZone) {
        return prefix.toLowerCase() + "-" + availabilityZone.substring(availabilityZone.lastIndexOf("-") + 1);
    }

    @Test
    void testBuildWhenBackendServiceExistsShouldUpdateWithNewBackends() throws Exception {
        // 1. Define parameters and the resource to be built/updated
        HealthProbeParameters healthProbe = new HealthProbeParameters(null, 8080, null, 0, 0);
        Map<String, Object> resourceParams = new HashMap<>();
        resourceParams.put("hcport", healthProbe);
        resourceParams.put("trafficports", List.of(new TargetGroupPortPair(80, 8080)));
        CloudResource buildableResource = CloudResource.builder()
                .withType(ResourceType.GCP_BACKEND_SERVICE)
                .withStatus(CommonStatus.CREATED)
                .withName("existing-service")
                .withParameters(resourceParams)
                .build();
        // 2. Define groups: one whose backend exists, one that is new
        Group existingGroup = mock(Group.class);
        when(existingGroup.getName()).thenReturn("master");

        // 3. Configure the load balancer to use both groups for the same port
        Map<TargetGroupPortPair, Set<Group>> lbMapping = new HashMap<>();
        lbMapping.put(new TargetGroupPortPair(80, 8080), Set.of(existingGroup));
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(lbMapping);
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);

        // 4. Mock the provider to return an existing BackendService with one backend
        String masterGroupName = groupName("stackName-master-111", AVAILABILITY_ZONE);
        String masterGroupNameB = groupName("stackName-master-111", AVAILABILITY_ZONE_B);
        String existingBackendUrl = String.format(GCP_INSTANCEGROUP_REFERENCE_FORMAT, PROJECT_ID, AVAILABILITY_ZONE, masterGroupName);
        Backend initialBackend = new Backend().setGroup(existingBackendUrl);
        BackendService backendServiceFromProvider = new BackendService()
                .setName(buildableResource.getName())
                .setBackends(List.of(initialBackend));

        Compute.RegionBackendServices.Get getRequest = mock(Compute.RegionBackendServices.Get.class);
        when(gcpContext.getCompute()).thenReturn(compute);
        when(compute.regionBackendServices()).thenReturn(regionBackendServices);
        when(regionBackendServices.get(PROJECT_ID, REGION, buildableResource.getName())).thenReturn(getRequest);
        when(getRequest.execute()).thenReturn(backendServiceFromProvider);

        // 5. Mock context and name generation details
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
        when(gcpContext.getName()).thenReturn("stackName");
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(region.getRegionName()).thenReturn(REGION);
        when(authenticatedContext.getCloudContext().getId()).thenReturn(111L);

        // 6. Mock instance group availability for both groups
        when(existingGroup.getNetwork()).thenReturn(groupNetwork);
        when(groupNetwork.getAvailabilityZones()).thenReturn(Set.of(AVAILABILITY_ZONE, AVAILABILITY_ZONE_B));
        when(compute.instanceGroups()).thenReturn(instanceGroups);
        mockInstanceGroupList(masterGroupName, masterGroupNameB);

        // 7. Mock the update call and capture the submitted BackendService
        Compute.RegionBackendServices.Update updateRequest = mock(Compute.RegionBackendServices.Update.class);
        ArgumentCaptor<BackendService> backendServiceCaptor = ArgumentCaptor.forClass(BackendService.class);
        when(regionBackendServices.update(eq(PROJECT_ID), eq(REGION), eq(buildableResource.getName()), backendServiceCaptor.capture()))
                .thenReturn(updateRequest);
        when(updateRequest.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("update-operation");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);

        // 8. Execute the build method
        List<CloudResource> result = underTest.build(gcpContext, authenticatedContext, List.of(buildableResource), cloudLoadBalancer, cloudStack);

        // 9. Assert the results
        assertEquals(1, result.size());
        assertEquals(buildableResource.getName(), result.get(0).getName());

        BackendService capturedBackendService = backendServiceCaptor.getValue();
        assertEquals(2, capturedBackendService.getBackends().size());

        String newBackendUrl = String.format(GCP_INSTANCEGROUP_REFERENCE_FORMAT, PROJECT_ID, AVAILABILITY_ZONE_B, masterGroupNameB);

        assertTrue(capturedBackendService.getBackends().stream().anyMatch(b -> b.getGroup().equals(existingBackendUrl)));
        assertTrue(capturedBackendService.getBackends().stream().anyMatch(b -> b.getGroup().equals(newBackendUrl)));
    }

    private void mockInstanceGroupList(String groupName, String groupNameB) throws java.io.IOException {
        InstanceGroupsListInstances listResult = mock(InstanceGroupsListInstances.class);
        when(listResult.getItems()).thenReturn(List.of(new InstanceWithNamedPorts()));
        Compute.InstanceGroups.ListInstances listInstances = mock(Compute.InstanceGroups.ListInstances.class);
        when(listInstances.execute()).thenReturn(listResult);
        when(instanceGroups.listInstances(eq(PROJECT_ID), eq(AVAILABILITY_ZONE), eq(groupName), any())).thenReturn(listInstances);
        when(instanceGroups.listInstances(eq(PROJECT_ID), eq(AVAILABILITY_ZONE_B), eq(groupNameB), any())).thenReturn(listInstances);
    }
}
