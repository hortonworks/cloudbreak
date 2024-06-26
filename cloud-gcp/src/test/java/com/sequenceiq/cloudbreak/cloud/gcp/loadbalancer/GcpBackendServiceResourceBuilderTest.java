package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.RegionBackendServices;
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
    private Operation operation;

    @Mock
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

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
        cloudStack = new CloudStack(Collections.emptyList(), network, image, emptyMap(), emptyMap(), null,
                null, null, null, null, null, null, null);
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

    @Test
    void testBuildWithSeparateHCPort() throws Exception {
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
                .build();

        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_BACKEND_SERVICE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withParameters(parameters)
                .withPersistent(true)
                .build();

        when(group.getName()).thenReturn("master");

        Compute.RegionBackendServices.Insert insert = mock(Compute.RegionBackendServices.Insert.class);

        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(80, 8080), Set.of(group));
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getName()).thenReturn("stackName");
        when(gcpContext.getProjectId()).thenReturn("id");
        when(gcpContext.getLoadBalancerResources(any())).thenReturn(List.of(hcResource));
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(availabilityZone.value()).thenReturn("us-west2");
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn("us-west2");

        when(compute.regionBackendServices()).thenReturn(regionBackendServices);
        when(regionBackendServices.insert(anyString(), anyString(), any())).thenReturn(insert);
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
                .build();

        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_BACKEND_SERVICE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withParameters(parameters)
                .withPersistent(true)
                .build();

        when(group.getName()).thenReturn("master");

        Compute.RegionBackendServices.Insert insert = mock(Compute.RegionBackendServices.Insert.class);

        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(443, 8443), Set.of(group));
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(11443, 8443), Set.of(group));
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getName()).thenReturn("stackName");
        when(gcpContext.getProjectId()).thenReturn("id");
        when(gcpContext.getLoadBalancerResources(any())).thenReturn(List.of(hcResource));
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(availabilityZone.value()).thenReturn("us-west2");
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn("us-west2");

        when(compute.regionBackendServices()).thenReturn(regionBackendServices);
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
}
