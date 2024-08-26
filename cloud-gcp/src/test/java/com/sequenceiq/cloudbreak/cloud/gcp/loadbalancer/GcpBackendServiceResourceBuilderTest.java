package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpBackendServiceResourceBuilder.GCP_INSTANCEGROUP_REFERENCE_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpBackendServiceResourceBuilderTest {

    private static final String AVAILABILITY_ZONE = "us-west2-c";

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

    private CloudStack cloudStack;

    @BeforeEach
    void setup() {
        Map<InstanceGroupType, String> userData = ImmutableMap.of(InstanceGroupType.CORE, "CORE", InstanceGroupType.GATEWAY, "GATEWAY");
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "", "default", "default-id", new HashMap<>(), "2019-10-24",
                1571884856L);
        GcpResourceNameService resourceNameService = new GcpResourceNameService();
        ReflectionTestUtils.setField(resourceNameService, "maxResourceNameLength", 50);
        ReflectionTestUtils.setField(underTest, "resourceNameService", resourceNameService);
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

        List<CloudResource> cloudResources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer);

        assertTrue(cloudResources.get(0).getName().startsWith("name-public-8080"));
        assertEquals(1, cloudResources.size());
        assertEquals(8080, cloudResources.get(0).getParameter("hcport", Integer.class));
        assertEquals(80, cloudResources.get(0).getParameter("trafficport", Integer.class));
    }

    static Object [] [] dataForInstanceGroups() {
        return new Object[] [] {
                {Map.of("us-west2-a", true, "us-west2-b", true, "us-west2-c", true)},
                {Map.of("us-west2-a", false, "us-west2-b", false, "us-west2-c", false)},
                {Map.of("us-west2-a", true, "us-west2-b", false, "us-west2-c", true)},
                {Map.of("us-west2-a", true, "us-west2-b", false)},
                {Map.of()}
        };
    }

    @ParameterizedTest(name = "testBuildWithSeparateHCPort{index}")
    @MethodSource("dataForInstanceGroups")
    void testBuildWithSeparateHCPort(Map<String, Boolean> instanceGroupsMap) throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", 8080);
        parameters.put("trafficport", 80);

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
        when(regionBackendServices.insert(eq(PROJECT_ID), eq(REGION), backendServiceArgumentCaptor.capture())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);
        when(gcpLoadBalancerTypeConverter.getScheme(any(CloudLoadBalancer.class))).thenCallRealMethod();
        when(authenticatedContext.getCloudContext().getId()).thenReturn(111L);

        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        assertEquals("super", cloudResources.get(0).getName());

        assertEquals(8080, cloudResources.get(0).getParameter("hcport", Integer.class));
        assertEquals(80, cloudResources.get(0).getParameter("trafficport", Integer.class));
        BackendService backendService = backendServiceArgumentCaptor.getValue();
        assertEquals(instanceGroupsMap.values().stream().filter(Boolean::booleanValue).count(), backendService.getBackends().size());
        for (Map.Entry<String, Boolean> entry : instanceGroupsMap.entrySet()) {
            assertEquals(entry.getValue(), backendService.getBackends().stream().anyMatch(
                    backend -> backend.getGroup().equals(String.format(GCP_INSTANCEGROUP_REFERENCE_FORMAT,
                    PROJECT_ID, entry.getKey(), groupName("stackname-master-111", entry.getKey())))));

        }

    }

    @Test
    void testBuildWithMultipleTrafficPorts() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", 8443);
        parameters.put("trafficports", List.of(443, 11443));

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
        when(regionBackendServices.insert(anyString(), anyString(), any())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);
        when(gcpLoadBalancerTypeConverter.getScheme(any(CloudLoadBalancer.class))).thenCallRealMethod();
        when(authenticatedContext.getCloudContext().getId()).thenReturn(111L);

        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        assertEquals("super", cloudResources.get(0).getName());

        assertEquals(8443, cloudResources.get(0).getParameter("hcport", Integer.class));
        List<Integer> ports = (List<Integer>) cloudResources.get(0).getParameters().get("trafficports");
        assertTrue(ports.contains(443));
        assertTrue(ports.contains(11443));
    }

    @Test
    void testDelete() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", 8080);
        parameters.put("trafficport", 80);

        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_BACKEND_SERVICE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withParameters(parameters)
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

        assertEquals(8080, cloudResource.getParameter("hcport", Integer.class));
        assertEquals(80, cloudResource.getParameter("trafficport", Integer.class));
        assertEquals(ResourceType.GCP_BACKEND_SERVICE, cloudResource.getType());
        assertEquals(CommonStatus.CREATED, cloudResource.getStatus());
    }

    private String groupName(String prefix, String availabilityZone) {
        return prefix + "-" + availabilityZone.substring(availabilityZone.lastIndexOf("-") + 1);
    }
}
