package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.ForwardingRule;
import com.google.api.services.compute.model.ForwardingRuleList;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class GCPLoadBalancerMetadataCollectorTest {

    private static final String FORWARDING_RULE_NAME_1 = "stack-public-80";

    private static final String FORWARDING_RULE_NAME_2 = "stack-private-80";

    private static final String PUBLIC_IP = "192.168.1.1";

    private static final String PRIVATE_IP = "10.10.1.1";

    private static final String PUBLIC_IP2 = "192.168.1.2";

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Mock
    private Compute compute;

    @Mock
    private Compute.ForwardingRules forwardingRules;

    @Mock
    private Compute.ForwardingRules.List forwardingRulesList;

    @Mock
    private ForwardingRuleList forwardingRuleListResponse;

    @InjectMocks
    private GcpMetadataCollector underTest;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    public void before() throws IOException {

        Location location = Location.location(Region.region("us-west2"), AvailabilityZone.availabilityZone("us-west2-a"));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withName("test-cluster")
                .withLocation(location)
                .withUserName("")
                .build();

        CloudCredential cloudCredential =
                new CloudCredential("1", "gcp-cred", Collections.singletonMap("projectId", "gcp-cred"), false);
        authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);

        when(gcpStackUtil.getProjectId(any())).thenReturn("gcp-cred");
        when(gcpComputeFactory.buildCompute(any())).thenReturn(compute);
        when(compute.forwardingRules()).thenReturn(forwardingRules);
        when(forwardingRules.list(anyString(), anyString())).thenReturn(forwardingRulesList);
        when(forwardingRulesList.execute()).thenReturn(forwardingRuleListResponse);
        lenient().when(gcpLoadBalancerTypeConverter.getScheme(anyString())).thenCallRealMethod();
    }

    @Test
    public void testCollectSinglePublicLoadBalancer() {
        List<CloudResource> resources = new ArrayList<>();
        resources.add(createCloudResource(FORWARDING_RULE_NAME_1, ResourceType.GCP_FORWARDING_RULE));
        ForwardingRule publicFowardingRule = createPublicFowardingRule();
        when(forwardingRuleListResponse.getItems()).thenReturn(List.of(publicFowardingRule));

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        assertEquals(1, result.size());
        assertEquals(PUBLIC_IP, result.get(0).getIp());
        assertEquals(LoadBalancerType.PUBLIC, result.get(0).getType());
    }

    @Test
    public void testCollectSinglePublicLoadBalancerWithMultipleForwardingRules() {

        List<CloudResource> resources = new ArrayList<>();
        resources.add(createCloudResource(FORWARDING_RULE_NAME_1, ResourceType.GCP_FORWARDING_RULE));
        resources.add(createCloudResource(FORWARDING_RULE_NAME_2, ResourceType.GCP_FORWARDING_RULE));
        ForwardingRule publicFowardingRule = createPublicFowardingRule();
        ForwardingRule publicFowardingRule2 = createPublicFowardingRule();
        publicFowardingRule2.setIPAddress(PUBLIC_IP2);
        publicFowardingRule2.setName(FORWARDING_RULE_NAME_2);
        when(forwardingRuleListResponse.getItems()).thenReturn(List.of(publicFowardingRule, publicFowardingRule2));

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        assertEquals(2, result.size());
        assertEquals(PUBLIC_IP, result.get(0).getIp());
        assertEquals(PUBLIC_IP2, result.get(1).getIp());
        assertEquals(LoadBalancerType.PUBLIC, result.get(0).getType());
    }

    @Test
    public void testCollectPublicAndPrivateLoadBalancer() {

        List<CloudResource> resources = new ArrayList<>();
        resources.add(createCloudResource(FORWARDING_RULE_NAME_1, ResourceType.GCP_FORWARDING_RULE));
        resources.add(createCloudResource(FORWARDING_RULE_NAME_2, ResourceType.GCP_FORWARDING_RULE));
        ForwardingRule publicFowardingRule = createPublicFowardingRule();
        ForwardingRule publicFowardingRule2 = createPrivateFowardingRule();
        when(forwardingRuleListResponse.getItems()).thenReturn(List.of(publicFowardingRule, publicFowardingRule2));

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext,
                List.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE), resources);

        assertEquals(2, result.size());

        Optional<CloudLoadBalancerMetadata> publicResult = result.stream().filter(p -> p.getType().equals(LoadBalancerType.PUBLIC)).findFirst();
        assertTrue(publicResult.isPresent());
        assertEquals(PUBLIC_IP, publicResult.get().getIp());

        Optional<CloudLoadBalancerMetadata> privateResult = result.stream().filter(p -> p.getType().equals(LoadBalancerType.PRIVATE)).findFirst();
        assertTrue(privateResult.isPresent());
        assertEquals(PRIVATE_IP, privateResult.get().getIp());

    }

    @Test
    public void testCollectPrivateLoadBalancer() {
        List<CloudResource> resources = new ArrayList<>();
        resources.add(createCloudResource(FORWARDING_RULE_NAME_2, ResourceType.GCP_FORWARDING_RULE));
        ForwardingRule privateForwardingRule = createPrivateFowardingRule();
        when(forwardingRuleListResponse.getItems()).thenReturn(List.of(privateForwardingRule));


        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PRIVATE), resources);

        assertEquals(1, result.size());
        assertEquals(PRIVATE_IP, result.get(0).getIp());
        assertEquals(LoadBalancerType.PRIVATE, result.get(0).getType());
    }

    @Test
    public void testCollectOtherType() {
        List<CloudResource> resources = new ArrayList<>();
        resources.add(createCloudResource(FORWARDING_RULE_NAME_2, ResourceType.GCP_FORWARDING_RULE));
        ForwardingRule privateForwardingRule = createPrivateFowardingRule();
        when(forwardingRuleListResponse.getItems()).thenReturn(List.of(privateForwardingRule));

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        assertEquals(0, result.size());
    }

    @Test
    public void testCollectLoadBalancerWithNoIps() {
        List<CloudResource> resources = new ArrayList<>();
        resources.add(createCloudResource(FORWARDING_RULE_NAME_1, ResourceType.GCP_FORWARDING_RULE));
        ForwardingRule forwardingRule = new ForwardingRule();
        forwardingRule.setLoadBalancingScheme("EXTERNAL");
        forwardingRule.setName(FORWARDING_RULE_NAME_1);
        when(forwardingRuleListResponse.getItems()).thenReturn(List.of(forwardingRule));

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        assertEquals(1, result.size());
        assertEquals(null, result.get(0).getIp());
        assertEquals(LoadBalancerType.PUBLIC, result.get(0).getType());
    }

    @Test
    public void testCollectLoadBalancerSkipsMetadataWhenRuntimeExceptionIsThrown() throws IOException {
        List<CloudResource> resources = new ArrayList<>();
        resources.add(createCloudResource(FORWARDING_RULE_NAME_1, ResourceType.GCP_FORWARDING_RULE));
        when(forwardingRulesList.execute()).thenThrow(new RuntimeException());


        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        assertEquals(0, result.size());
    }

    private ForwardingRule createPublicFowardingRule() {
        ForwardingRule forwardingRule = new ForwardingRule();
        forwardingRule.setLoadBalancingScheme("EXTERNAL");
        forwardingRule.setName(FORWARDING_RULE_NAME_1);
        forwardingRule.setIPAddress(PUBLIC_IP);
        return forwardingRule;
    }

    private ForwardingRule createPrivateFowardingRule() {
        ForwardingRule forwardingRule = new ForwardingRule();
        forwardingRule.setLoadBalancingScheme("INTERNAL");
        forwardingRule.setName(FORWARDING_RULE_NAME_2);
        forwardingRule.setIPAddress(PRIVATE_IP);
        return forwardingRule;
    }

    static CloudResource createCloudResource(String name, ResourceType type) {
        return CloudResource.builder()
                .name(name)
                .type(type)
                .status(CommonStatus.CREATED)
                .params(Collections.emptyMap())
                .build();
    }

}
