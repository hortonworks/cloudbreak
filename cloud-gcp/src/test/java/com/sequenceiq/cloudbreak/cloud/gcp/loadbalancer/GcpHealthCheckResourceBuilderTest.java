package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.RegionHealthChecks;
import com.google.api.services.compute.model.HealthCheck;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpHealthCheckResourceBuilderTest {

    @Mock
    private GcpContext gcpContext;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudLoadBalancer cloudLoadBalancer;

    @InjectMocks
    private GcpHealthCheckResourceBuilder underTest;

    @Mock
    private Compute compute;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private RegionHealthChecks regionHealthChecks;

    @Mock
    private Operation operation;

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
    void testCreateWhenEverythingGoesFine() {
        when(gcpContext.getName()).thenReturn("name");
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(80, 8080), Collections.emptySet());
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);

        List<CloudResource> cloudResources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, cloudStack.getNetwork());

        assertTrue(cloudResources.get(0).getName().startsWith("name-public-8080"));
        assertEquals(1, cloudResources.size());
        assertEquals(8080, cloudResources.get(0).getParameter("hcport", HealthProbeParameters.class).getPort());
    }

    @Test
    void testBuild() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", new HealthProbeParameters(null, 8080, null, 0, 0));
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_HEALTH_CHECK)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withParameters(parameters)
                .withPersistent(true)
                .build();
        Compute.RegionHealthChecks.Insert healthCheckInsert = mock(Compute.RegionHealthChecks.Insert.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn("us-west2");

        when(compute.regionHealthChecks()).thenReturn(regionHealthChecks);
        when(regionHealthChecks.insert(anyString(), anyString(), any())).thenReturn(healthCheckInsert);
        when(healthCheckInsert.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);

        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        assertEquals("super", cloudResources.get(0).getName());
        assertEquals(8080, cloudResources.get(0).getParameter("hcport", HealthProbeParameters.class).getPort());

        ArgumentCaptor<HealthCheck> healthCheckArgumentCaptor = ArgumentCaptor.forClass(HealthCheck.class);
        verify(regionHealthChecks).insert(anyString(), anyString(), healthCheckArgumentCaptor.capture());
        HealthCheck healthCheckParam = healthCheckArgumentCaptor.getValue();
        assertEquals("TCP", healthCheckParam.getType());
        assertEquals(8080, healthCheckParam.getTcpHealthCheck().getPort());
    }

    @Test
    void testBuildWithHttpsHealth() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", new HealthProbeParameters("health", 8080, NetworkProtocol.HTTPS, 0, 0));
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_HEALTH_CHECK)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withParameters(parameters)
                .withPersistent(true)
                .build();
        Compute.RegionHealthChecks.Insert healthCheckInsert = mock(Compute.RegionHealthChecks.Insert.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn("us-west2");

        when(compute.regionHealthChecks()).thenReturn(regionHealthChecks);
        when(regionHealthChecks.insert(anyString(), anyString(), any())).thenReturn(healthCheckInsert);
        when(healthCheckInsert.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);

        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        assertEquals("super", cloudResources.get(0).getName());
        HealthProbeParameters healthCheck = cloudResources.get(0).getParameter("hcport", HealthProbeParameters.class);
        assertEquals(8080, healthCheck.getPort());
        assertEquals("health", healthCheck.getPath());
        ArgumentCaptor<HealthCheck> healthCheckArgumentCaptor = ArgumentCaptor.forClass(HealthCheck.class);
        verify(regionHealthChecks).insert(anyString(), anyString(), healthCheckArgumentCaptor.capture());
        HealthCheck healthCheckParam = healthCheckArgumentCaptor.getValue();
        assertEquals("HTTPS", healthCheckParam.getType());
        assertEquals(8080, healthCheckParam.getHttpsHealthCheck().getPort());
        assertEquals("health", healthCheckParam.getHttpsHealthCheck().getRequestPath());
    }

    @Test
    void testDelete() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", 8080);
        parameters.put("trafficport", 8080);
        CloudResource resource = CloudResource.builder()
                .withType(ResourceType.GCP_HEALTH_CHECK)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withParameters(parameters)
                .withPersistent(true)
                .build();

        Compute.RegionHealthChecks.Delete healthCheckDelete = mock(Compute.RegionHealthChecks.Delete.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn("us-west2");

        when(compute.regionHealthChecks()).thenReturn(regionHealthChecks);
        when(regionHealthChecks.delete(anyString(), anyString(), any())).thenReturn(healthCheckDelete);
        when(healthCheckDelete.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");

        CloudResource delete = underTest.delete(gcpContext, authenticatedContext, resource);

        assertEquals(ResourceType.GCP_HEALTH_CHECK, delete.getType());
        assertEquals(CommonStatus.CREATED, delete.getStatus());
        assertEquals("super", delete.getName());
    }
}
