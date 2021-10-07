package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
public class GcpBackendServiceResourceBuilderTest {

    @Mock
    private GcpContext gcpContext;

    @Mock
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

    private Image image;

    private CloudStack cloudStack;

    @BeforeEach
    private void setup() {
        Map<InstanceGroupType, String> userData = ImmutableMap.of(InstanceGroupType.CORE, "CORE", InstanceGroupType.GATEWAY, "GATEWAY");
        image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());
        GcpResourceNameService resourceNameService = new GcpResourceNameService();
        ReflectionTestUtils.setField(resourceNameService, "maxResourceNameLength", 50);
        ReflectionTestUtils.setField(underTest, "resourceNameService", resourceNameService);
        Network network = new Network(null);
        cloudStack = new CloudStack(Collections.emptyList(), network, image, emptyMap(), emptyMap(), null,
                null, null, null, null);
    }

    @Test
    public void testCreateWhereEverythingGoesFine() {
        when(gcpContext.getName()).thenReturn("name");
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(80, 8080), Collections.emptySet());
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);

        List<CloudResource> cloudResources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer);

        Assertions.assertTrue(cloudResources.get(0).getName().startsWith("name-public-8080"));
        Assertions.assertEquals(1, cloudResources.size());
        Assertions.assertEquals(8080, cloudResources.get(0).getParameter("hcport", Integer.class));
        Assertions.assertEquals(80, cloudResources.get(0).getParameter("trafficport", Integer.class));
    }

    @Test
    public void testBuildWithSeperateHCPort() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", 8080);
        parameters.put("trafficport", 80);

        CloudResource hcResource = new CloudResource.Builder()
                .type(ResourceType.GCP_HEALTH_CHECK)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("hcsuper")
                .params(parameters)
                .persistent(true)
                .build();

        CloudResource resource = new CloudResource.Builder()
                .type(ResourceType.GCP_BACKEND_SERVICE)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("super")
                .params(parameters)
                .persistent(true)
                .build();

        when(group.getName()).thenReturn("master");

        Compute.RegionBackendServices.Insert insert = mock(Compute.RegionBackendServices.Insert.class);

        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(80, 8080), Set.of(group));
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);

        when(gcpContext.getCompute()).thenReturn(compute);
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

        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        Assert.assertEquals("super", cloudResources.get(0).getName());

        Assertions.assertEquals(8080, cloudResources.get(0).getParameter("hcport", Integer.class));
        Assertions.assertEquals(80, cloudResources.get(0).getParameter("trafficport", Integer.class));
    }

    @Test
    public void testBuildWithMultipleTrafficPorts() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", 8443);
        parameters.put("trafficports", List.of(443, 11443));

        CloudResource hcResource = new CloudResource.Builder()
                .type(ResourceType.GCP_HEALTH_CHECK)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("hcsuper")
                .params(parameters)
                .persistent(true)
                .build();

        CloudResource resource = new CloudResource.Builder()
                .type(ResourceType.GCP_BACKEND_SERVICE)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("super")
                .params(parameters)
                .persistent(true)
                .build();

        when(group.getName()).thenReturn("master");

        Compute.RegionBackendServices.Insert insert = mock(Compute.RegionBackendServices.Insert.class);

        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(443, 8443), Set.of(group));
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(11443, 8443), Set.of(group));
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);

        when(gcpContext.getCompute()).thenReturn(compute);
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

        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        Assert.assertEquals("super", cloudResources.get(0).getName());

        Assertions.assertEquals(8443, cloudResources.get(0).getParameter("hcport", Integer.class));
        List<Integer> ports = (List<Integer>) cloudResources.get(0).getParameters().get("trafficports");
        Assertions.assertTrue(ports.contains(443));
        Assertions.assertTrue(ports.contains(11443));
    }

    @Test
    public void testDelete() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", 8080);
        parameters.put("trafficport", 80);

        CloudResource resource = new CloudResource.Builder()
                .type(ResourceType.GCP_BACKEND_SERVICE)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("super")
                .params(parameters)
                .persistent(true)
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

        Assert.assertEquals("super", cloudResource.getName());

        Assertions.assertEquals(8080, cloudResource.getParameter("hcport", Integer.class));
        Assertions.assertEquals(80, cloudResource.getParameter("trafficport", Integer.class));
        Assert.assertEquals(ResourceType.GCP_BACKEND_SERVICE, cloudResource.getType());
        Assert.assertEquals(CommonStatus.CREATED, cloudResource.getStatus());
    }
}
