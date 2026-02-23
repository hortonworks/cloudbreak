package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.AbstractGcpLoadBalancerBuilder.HCPORT;
import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.AbstractGcpLoadBalancerBuilder.TRAFFICPORTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.HealthProbeParameters;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpForwardingRuleResourceBuilderCreateTest {

    private static final long STACK_ID = 1770206795022L;

    private static final String STACK_NAME = "gcpenvfreeipa";

    @Spy
    @InjectMocks
    private GcpForwardingRuleResourceBuilder underTest;

    @Mock
    private GcpContext gcpContext;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudLoadBalancer cloudLoadBalancer;

    @Mock
    private Network network;

    @Mock
    private CloudContext cloudContext;

    private GcpResourceNameService resourceNameService;

    @BeforeEach
    void setUp() {
        resourceNameService = new GcpResourceNameService();
        ReflectionTestUtils.setField(resourceNameService, "maxResourceNameLength", 100);

        lenient().when(gcpContext.getName()).thenReturn(STACK_NAME);
        lenient().when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        lenient().when(cloudContext.getId()).thenReturn(STACK_ID);

        ReflectionTestUtils.setField(underTest, "resourceNameService", resourceNameService);
        underTest.init();
    }

    @Test
    void testCreateForPrivateLoadBalancerWithMixedExistingResources() {
        // 1. Configure mocks for a private load balancer
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);

        // 2. Define HealthProbeParameters to be used consistently, ensuring a non-null protocol
        HealthProbeParameters healthProbeFor5080 = new HealthProbeParameters(null, 5080, NetworkProtocol.TCP, 0, 0);
        HealthProbeParameters healthProbeFor9090 = new HealthProbeParameters(null, 9090, NetworkProtocol.TCP, 0, 0);
        HealthProbeParameters healthProbeFor9999 = new HealthProbeParameters(null, 9999, NetworkProtocol.TCP, 0, 0);

        // 3. Init existing resources
        CloudResource forwardingRuleNoProto = CloudResource.builder()
                .withType(ResourceType.GCP_FORWARDING_RULE)
                .withStatus(CommonStatus.CREATED)
                .withName("gcpenvfreeipa-private-5080-1770206795022")
                .withParameters(Map.of(
                        "hcport", healthProbeFor5080,
                        CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.PRIVATE.asMap()
                ))
                .build();

        CloudResource forwardingRuleWithProto = CloudResource.builder()
                .withType(ResourceType.GCP_FORWARDING_RULE)
                .withStatus(CommonStatus.CREATED)
                .withName("gcpenvfreeipa-private-tcp-9090-1770206795022")
                .withParameters(Map.of(
                        "hcport", healthProbeFor9090,
                        CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.PRIVATE.asMap()
                ))
                .build();

        CloudResource forwardingRuleForPublicLB = CloudResource.builder()
                .withType(ResourceType.GCP_FORWARDING_RULE)
                .withStatus(CommonStatus.CREATED)
                .withName("gcpenvfreeipa-public-443-1770206795022")
                .withParameters(Map.of(
                        "hcport", new HealthProbeParameters(null, 443, NetworkProtocol.TCP, 0, 0),
                        CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.PUBLIC.asMap()
                ))
                .build();

        // 4. Stub the fetchAllResourceFromDb calls to satisfy strict stubbing
        doReturn(List.of(forwardingRuleNoProto, forwardingRuleWithProto, forwardingRuleForPublicLB))
                .when(underTest).fetchAllResourceFromDb(eq(ResourceType.GCP_FORWARDING_RULE), any());

        // 5. Configure the input CloudLoadBalancer with TargetGroupPortPairs, all having a non-null protocol
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(Map.of(
                new TargetGroupPortPair(100, NetworkProtocol.TCP, healthProbeFor5080), Set.of(),
                new TargetGroupPortPair(200, NetworkProtocol.TCP, healthProbeFor9090), Set.of(),
                new TargetGroupPortPair(300, NetworkProtocol.TCP, healthProbeFor9999), Set.of(),
                new TargetGroupPortPair(400, NetworkProtocol.TCP, healthProbeFor9999), Set.of(),
                new TargetGroupPortPair(53, NetworkProtocol.TCP_UDP, healthProbeFor9999), Set.of(),
                new TargetGroupPortPair(88, NetworkProtocol.UDP, healthProbeFor9999), Set.of()
        ));

        // 6. Execute the create method
        List<CloudResource> createdFwRules = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, network);

        // 7. Assert the results
        assertEquals(4, createdFwRules.size());

        Optional<CloudResource> fwRuleForPort100 = createdFwRules.stream()
                .filter(r -> r.getName().equals(forwardingRuleNoProto.getName())).findFirst();
        assertTrue(fwRuleForPort100.isPresent());
        assertEquals(CommonStatus.CREATED, fwRuleForPort100.get().getStatus());

        Optional<CloudResource> fwRuleForPort200 = createdFwRules.stream()
                .filter(r -> r.getName().equals(forwardingRuleWithProto.getName())).findFirst();
        assertTrue(fwRuleForPort200.isPresent());
        assertEquals(CommonStatus.CREATED, fwRuleForPort200.get().getStatus());

        Optional<CloudResource> fwRuleForPort9999Tcp = createdFwRules.stream()
                .filter(r -> r.getName().contains("-tcp-9999-")).findFirst();
        assertTrue(fwRuleForPort9999Tcp.isPresent());
        assertEquals(CommonStatus.CREATED, fwRuleForPort9999Tcp.get().getStatus());
        GcpLBTraffics gcpLBTraffics = fwRuleForPort9999Tcp.get().getParameter(TRAFFICPORTS, GcpLBTraffics.class);
        assertEquals(NetworkProtocol.TCP, gcpLBTraffics.trafficProtocol());
        assertEquals(Set.of(300, 400, 53), gcpLBTraffics.trafficPorts());
        HealthProbeParameters healthProbeParameters = fwRuleForPort9999Tcp.get().getParameter(HCPORT, HealthProbeParameters.class);
        assertEquals(healthProbeFor9999, healthProbeParameters);


        Optional<CloudResource> fwRuleForPort9999Udp = createdFwRules.stream()
                .filter(r -> r.getName().contains("-udp-9999-")).findFirst();
        assertTrue(fwRuleForPort9999Udp.isPresent());
        assertEquals(CommonStatus.CREATED, fwRuleForPort9999Udp.get().getStatus());
        GcpLBTraffics gcpLBTrafficsUdp = fwRuleForPort9999Udp.get().getParameter(TRAFFICPORTS, GcpLBTraffics.class);
        assertEquals(NetworkProtocol.UDP, gcpLBTrafficsUdp.trafficProtocol());
        assertEquals(Set.of(53, 88), gcpLBTrafficsUdp.trafficPorts());
        HealthProbeParameters healthProbeParametersUdp = fwRuleForPort9999Udp.get().getParameter(HCPORT, HealthProbeParameters.class);
        assertEquals(healthProbeFor9999, healthProbeParametersUdp);
    }
}
