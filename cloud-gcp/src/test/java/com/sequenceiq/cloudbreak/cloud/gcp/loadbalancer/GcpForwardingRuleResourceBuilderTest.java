package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.ForwardingRule;
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
class GcpForwardingRuleResourceBuilderTest {

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
    private Compute.Subnetworks subnetworks;

    @Mock
    private Group group;

    @Mock
    private Operation operation;

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Captor
    private ArgumentCaptor<ForwardingRule> forwardingRuleArg;

    @Mock
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    private CloudStack cloudStack;

    private CloudResource resource;

    private CloudResource backendResource;

    private CloudResource ipResource;

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
                .image(image)
                .network(network)
                .build();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", new HealthProbeParameters(null, 8080, null, 0, 0));
        parameters.put("trafficports", new GcpLBTraffics(List.of(8080), null));
        resource = CloudResource.builder()
                .withType(ResourceType.GCP_FORWARDING_RULE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withParameters(parameters)
                .withPersistent(true)
                .build();

        backendResource = CloudResource.builder()
                .withType(ResourceType.GCP_BACKEND_SERVICE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("backendsuper")
                .withParameters(parameters)
                .withPersistent(true)
                .build();

        ipResource = CloudResource.builder()
                .withType(ResourceType.GCP_RESERVED_IP)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("ipsuper")
                .withPersistent(true)
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

        assertEquals(1, cloudResources.size());
        assertTrue(cloudResources.get(0).getName().startsWith("name-public-tcp-80"));
        assertEquals(8080, cloudResources.get(0).getParameter("hcport", HealthProbeParameters.class).getPort());
        GcpLBTraffics traffics = cloudResources.get(0).getParameter("trafficports", GcpLBTraffics.class);
        assertTrue(traffics.trafficPorts().contains(80));
    }

    @Test
    void testCreatePrivate() {
        when(gcpContext.getName()).thenReturn("name");
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);
        Map<TargetGroupPortPair, Set<Group>> targetGroupPortPairSetHashMap = new HashMap<>();
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(80, NetworkProtocol.TCP, new HealthProbeParameters(null, 8080, null, 0, 0)),
                Collections.emptySet());
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(81, NetworkProtocol.TCP, new HealthProbeParameters(null, 8080, null, 0, 0)),
                Collections.emptySet());
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(80, NetworkProtocol.UDP, new HealthProbeParameters(null, 8080, null, 0, 0)),
                Collections.emptySet());
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(81, NetworkProtocol.UDP, new HealthProbeParameters(null, 8080, null, 0, 0)),
                Collections.emptySet());
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(82, NetworkProtocol.TCP, new HealthProbeParameters(null, 8081, null, 0, 0)),
                Collections.emptySet());
        targetGroupPortPairSetHashMap.put(new TargetGroupPortPair(82, NetworkProtocol.UDP, new HealthProbeParameters(null, 8081, null, 0, 0)),
                Collections.emptySet());
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(targetGroupPortPairSetHashMap);

        List<CloudResource> cloudResources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, cloudStack.getNetwork());

        List<CloudResource> forwardingRulesTcp8080 = cloudResources.stream()
                .filter(cloudResource ->
                        cloudResource.getParameter("trafficports", GcpLBTraffics.class).trafficProtocol() == NetworkProtocol.TCP &&
                                cloudResource.getParameter("hcport", HealthProbeParameters.class).getPort() == 8080)
                .toList();
        assertEquals(1, forwardingRulesTcp8080.size());
        GcpLBTraffics gcpLBTcpTraffics8080 = forwardingRulesTcp8080.get(0).getParameter("trafficports", GcpLBTraffics.class);
        assertTrue(forwardingRulesTcp8080.get(0).getName().startsWith("name-private-tcp"));
        assertEquals(NetworkProtocol.TCP, gcpLBTcpTraffics8080.trafficProtocol());
        assertTrue(gcpLBTcpTraffics8080.trafficPorts().contains(80));
        assertTrue(gcpLBTcpTraffics8080.trafficPorts().contains(81));

        List<CloudResource> forwardingRulesUdp8080 = cloudResources.stream()
                .filter(cloudResource ->
                        cloudResource.getParameter("trafficports", GcpLBTraffics.class).trafficProtocol() == NetworkProtocol.UDP &&
                                cloudResource.getParameter("hcport", HealthProbeParameters.class).getPort() == 8080)
                .toList();
        assertEquals(1, forwardingRulesUdp8080.size());
        assertTrue(forwardingRulesUdp8080.get(0).getName().startsWith("name-private-udp"));
        GcpLBTraffics gcpLBUdpTraffics8080 = forwardingRulesUdp8080.get(0).getParameter("trafficports", GcpLBTraffics.class);
        assertEquals(NetworkProtocol.UDP, gcpLBUdpTraffics8080.trafficProtocol());
        assertTrue(gcpLBUdpTraffics8080.trafficPorts().contains(80));
        assertTrue(gcpLBUdpTraffics8080.trafficPorts().contains(81));

        List<CloudResource> forwardingRulesTcp8081 = cloudResources.stream()
                .filter(cloudResource ->
                        cloudResource.getParameter("trafficports", GcpLBTraffics.class).trafficProtocol() == NetworkProtocol.TCP &&
                                cloudResource.getParameter("hcport", HealthProbeParameters.class).getPort() == 8081)
                .toList();
        assertEquals(1, forwardingRulesTcp8081.size());
        GcpLBTraffics gcpLBTcpTraffics8081 = forwardingRulesTcp8081.get(0).getParameter("trafficports", GcpLBTraffics.class);
        assertTrue(forwardingRulesTcp8081.get(0).getName().startsWith("name-private-tcp"));
        assertEquals(NetworkProtocol.TCP, gcpLBTcpTraffics8081.trafficProtocol());
        assertTrue(gcpLBTcpTraffics8081.trafficPorts().contains(82));

        List<CloudResource> forwardingRulesUdp8081 = cloudResources.stream()
                .filter(cloudResource ->
                        cloudResource.getParameter("trafficports", GcpLBTraffics.class).trafficProtocol() == NetworkProtocol.UDP &&
                                cloudResource.getParameter("hcport", HealthProbeParameters.class).getPort() == 8081)
                .toList();
        assertEquals(1, forwardingRulesTcp8081.size());
        assertTrue(forwardingRulesUdp8081.get(0).getName().startsWith("name-private-udp"));
        GcpLBTraffics gcpLBUdpTraffics8081 = forwardingRulesUdp8081.get(0).getParameter("trafficports", GcpLBTraffics.class);
        assertEquals(NetworkProtocol.UDP, gcpLBUdpTraffics8081.trafficProtocol());
        assertTrue(gcpLBUdpTraffics8081.trafficPorts().contains(82));
    }

    @Test
    void testBuild() throws Exception {
        mockCalls(LoadBalancerType.PRIVATE);

        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        ForwardingRule arg = forwardingRuleArg.getValue();
        assertEquals("https://www.googleapis.com/compute/v1/projects/id/global/networks/default-network", arg.getNetwork());
        assertEquals("https://www.googleapis.com/compute/v1/projects/id/regions/us-west2/subnetworks/default-subnet", arg.getSubnetwork());
        assertEquals("super", cloudResources.get(0).getName());
        assertEquals(8080, cloudResources.get(0).getParameter("hcport", HealthProbeParameters.class).getPort());
    }

    @Test
    void buildForPublic() throws Exception {
        mockCalls(LoadBalancerType.PUBLIC);
        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        ForwardingRule arg = forwardingRuleArg.getValue();
        assertNull(arg.getNetwork());
        assertNull(arg.getSubnetwork());
        assertEquals("super", cloudResources.get(0).getName());
        assertEquals(8080, cloudResources.get(0).getParameter("hcport", HealthProbeParameters.class).getPort());
    }

    @Test
    void buildForGatewayPrivate() throws Exception {
        mockCalls(LoadBalancerType.GATEWAY_PRIVATE);
        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        ForwardingRule arg = forwardingRuleArg.getValue();
        assertEquals("https://www.googleapis.com/compute/v1/projects/id/global/networks/default-network", arg.getNetwork());
        assertEquals("https://www.googleapis.com/compute/v1/projects/id/regions/us-west2/subnetworks/default-gw-subnet", arg.getSubnetwork());
        assertEquals("super", cloudResources.get(0).getName());
        assertEquals(8080, cloudResources.get(0).getParameter("hcport", HealthProbeParameters.class).getPort());
    }

    @Test
    void buildforSharedVPC() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hcport", new HealthProbeParameters(null, 8443, null, 0, 0));
        parameters.put("trafficports", new GcpLBTraffics(List.of(443, 11443), null));
        resource = CloudResource.builder()
                .withType(ResourceType.GCP_FORWARDING_RULE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("super")
                .withParameters(parameters)
                .withPersistent(true)
                .build();

        backendResource = CloudResource.builder()
                .withType(ResourceType.GCP_BACKEND_SERVICE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("master")
                .withName("backendsuper")
                .withParameters(parameters)
                .withPersistent(true)
                .build();

        mockCalls(LoadBalancerType.PRIVATE);
        when(gcpStackUtil.getSharedProjectId(any())).thenReturn("custom-project");

        List<CloudResource> cloudResources = underTest.build(gcpContext, authenticatedContext,
                Collections.singletonList(resource), cloudLoadBalancer, cloudStack);

        ForwardingRule arg = forwardingRuleArg.getValue();
        assertEquals("https://www.googleapis.com/compute/v1/projects/custom-project/global/networks/default-network", arg.getNetwork());
        assertEquals("https://www.googleapis.com/compute/v1/projects/custom-project/regions/us-west2/subnetworks/default-subnet", arg.getSubnetwork());
        assertEquals("super", cloudResources.get(0).getName());
        assertEquals(8443, cloudResources.get(0).getParameter("hcport", HealthProbeParameters.class).getPort());
        Collection<Integer> ports = cloudResources.get(0).getParameter("trafficports", GcpLBTraffics.class).trafficPorts();
        assertTrue(ports.contains(443));
        assertTrue(ports.contains(11443));
    }

    @Test
    void testDelete() throws Exception {
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

        assertEquals(ResourceType.GCP_FORWARDING_RULE, delete.getType());
        assertEquals(CommonStatus.CREATED, delete.getStatus());
        assertEquals("super", delete.getName());
    }

    private void mockCalls(LoadBalancerType lbType) throws IOException {
        Compute.ForwardingRules.Insert forwardingRulesInsert = mock(Compute.ForwardingRules.Insert.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn("us-west2");
        when(gcpContext.getLoadBalancerResources(any())).thenReturn(List.of(backendResource, ipResource));
        when(compute.forwardingRules()).thenReturn(forwardingRules);
        when(forwardingRules.insert(anyString(), anyString(), forwardingRuleArg.capture())).thenReturn(forwardingRulesInsert);
        when(forwardingRulesInsert.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);
        when(cloudLoadBalancer.getType()).thenReturn(lbType);
        lenient().when(gcpStackUtil.getCustomNetworkId(any())).thenReturn("default-network");
        lenient().when(gcpStackUtil.getSubnetId(any())).thenReturn("default-subnet");
        lenient().when(gcpStackUtil.getEndpointGatewaySubnetId(any())).thenReturn("default-gw-subnet");
        lenient().when(gcpStackUtil.getNetworkUrl(anyString(), anyString())).thenCallRealMethod();
        lenient().when(gcpStackUtil.getSubnetUrl(anyString(), anyString(), anyString())).thenCallRealMethod();
        lenient().when(gcpLoadBalancerTypeConverter.getScheme(any(CloudLoadBalancer.class))).thenCallRealMethod();
    }
}
