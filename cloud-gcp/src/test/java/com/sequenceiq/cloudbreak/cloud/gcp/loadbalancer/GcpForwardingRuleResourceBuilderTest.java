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
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
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
public class GcpForwardingRuleResourceBuilderTest {
    @Mock
    private GcpContext gcpContext;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudLoadBalancer cloudLoadBalancer;

    @InjectMocks
    private GcpForwardingRuleResourceBuilder underTest;

    @Mock
    private Compute compute;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private AvailabilityZone availabilityZone;

    @Mock
    private Compute.ForwardingRules forwardingRules;

    @Mock
    private Group group;

    @Mock
    private Operation operation;

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    @Mock
    private GcpStackUtil gcpStackUtil;

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

        Assertions.assertTrue(cloudResources.get(0).getName().startsWith("name-public-80"));
        Assertions.assertEquals(1, cloudResources.size());
        Assertions.assertEquals(8080, cloudResources.get(0).getParameter("hcport", Integer.class));
        Assertions.assertEquals(80, cloudResources.get(0).getParameter("trafficport", Integer.class));
    }

    @Test
    public void testBuild() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", 8080);
        parameters.put("trafficport", 8080);
        CloudResource resource = new CloudResource.Builder()
                .type(ResourceType.GCP_FORWARDING_RULE)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("super")
                .params(parameters)
                .persistent(true)
                .build();

        CloudResource backendResource = new CloudResource.Builder()
                .type(ResourceType.GCP_BACKEND_SERVICE)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("backendsuper")
                .params(parameters)
                .persistent(true)
                .build();

        CloudResource ipResource = new CloudResource.Builder()
                .type(ResourceType.GCP_RESERVED_IP)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("ipsuper")
                .persistent(true)
                .build();
        Compute.ForwardingRules.Insert forwardingRulesInsert = mock(Compute.ForwardingRules.Insert.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn("us-west2");
        when(gcpContext.getLoadBalancerResources(any())).thenReturn(List.of(backendResource, ipResource));

        when(compute.forwardingRules()).thenReturn(forwardingRules);
        when(forwardingRules.insert(anyString(), anyString(), any())).thenReturn(forwardingRulesInsert);
        when(forwardingRulesInsert.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);
        when(gcpStackUtil.getCustomNetworkId(any())).thenReturn("default-network");
        when(gcpStackUtil.getSubnetId(any())).thenReturn("default-subnet");


        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        Assert.assertEquals("super", cloudResources.get(0).getName());
        Assertions.assertEquals(8080, cloudResources.get(0).getParameter("hcport", Integer.class));

    }

    @Test
    public void testDelete() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", 8080);
        parameters.put("trafficport", 8080);
        CloudResource resource = new CloudResource.Builder()
                .type(ResourceType.GCP_FORWARDING_RULE)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("super")
                .params(parameters)
                .persistent(true)
                .build();

        Compute.ForwardingRules.Delete forwardingRulesDelete = mock(Compute.ForwardingRules.Delete.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn("us-west2");

        when(compute.forwardingRules()).thenReturn(forwardingRules);
        when(forwardingRules.delete(anyString(), anyString(), any())).thenReturn(forwardingRulesDelete);
        when(forwardingRulesDelete.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");

        CloudResource delete = underTest.delete(gcpContext, authenticatedContext, resource);

        Assert.assertEquals(ResourceType.GCP_FORWARDING_RULE, delete.getType());
        Assert.assertEquals(CommonStatus.CREATED, delete.getStatus());
        Assert.assertEquals("super", delete.getName());
    }

}
