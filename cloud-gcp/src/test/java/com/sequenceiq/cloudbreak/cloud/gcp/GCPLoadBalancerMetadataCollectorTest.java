package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import com.google.api.services.compute.model.Backend;
import com.google.api.services.compute.model.BackendService;
import com.google.api.services.compute.model.ForwardingRule;
import com.google.api.services.compute.model.ForwardingRuleList;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.view.GcpLoadBalancerMetadataView;
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

    private static final String PORT1 = "8080";

    private static final String PORT2 = "443";

    private static final String BACKEND_SERVICE_NAME = "backend-service-1";

    private static final String INSTANCE_GROUP_NAME = "instance-group-1";

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

    @Mock
    private Compute.RegionBackendServices backendServices;

    @Mock
    private Compute.RegionBackendServices.Get backendServicesGet;

    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    public void before() throws IOException {

        Location location = Location.location(Region.region("us-west2"), AvailabilityZone.availabilityZone("us-west2-a"));
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withName("test-cluster")
                .withLocation(location)
                .withUserName("")
                .build();

        CloudCredential cloudCredential = new CloudCredential("1", "gcp-cred", Collections.singletonMap("projectId", "gcp-cred"), "acc");
        authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);

        when(gcpStackUtil.getProjectId(any())).thenReturn("gcp-cred");
        when(gcpComputeFactory.buildCompute(any())).thenReturn(compute);
        when(compute.forwardingRules()).thenReturn(forwardingRules);
        when(forwardingRules.list(anyString(), anyString())).thenReturn(forwardingRulesList);
        when(forwardingRulesList.execute()).thenReturn(forwardingRuleListResponse);
        lenient().when(gcpLoadBalancerTypeConverter.getScheme(anyString())).thenCallRealMethod();

        lenient().when(compute.regionBackendServices()).thenReturn(backendServices);
        lenient().when(backendServices.get(anyString(), anyString(), eq(BACKEND_SERVICE_NAME))).thenReturn(backendServicesGet);
        lenient().when(backendServicesGet.execute()).thenReturn(createBackendService());
    }

    @Test
    public void testCollectLoadBalancerWhenGcpResponseReturnsWithEmptyResponse() {
        List<CloudResource> resources = new ArrayList<>();
        resources.add(createCloudResource(FORWARDING_RULE_NAME_1, ResourceType.GCP_FORWARDING_RULE));
        when(forwardingRuleListResponse.isEmpty()).thenReturn(Boolean.TRUE);

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollectLoadBalancerWhenGcpResponseReturnsWithNotEmptyResponseButTheGetItemsReturnWithNull() {
        List<CloudResource> resources = new ArrayList<>();
        resources.add(createCloudResource(FORWARDING_RULE_NAME_1, ResourceType.GCP_FORWARDING_RULE));
        when(forwardingRuleListResponse.isEmpty()).thenReturn(Boolean.FALSE);
        when(forwardingRuleListResponse.getItems()).thenReturn(null);

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        assertTrue(result.isEmpty());
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
        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getName());
        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getStringParameter(GcpLoadBalancerMetadataView.LOADBALANCER_NAME));
        assertEquals(LoadBalancerType.PUBLIC, result.get(0).getType());
        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getBackendServiceParam(8080)));
        assertEquals(INSTANCE_GROUP_NAME, result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getInstanceGroupParam(8080)));
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
        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getName());
        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getStringParameter(GcpLoadBalancerMetadataView.LOADBALANCER_NAME));

        assertEquals(BACKEND_SERVICE_NAME, result.get(1).getName());
        assertEquals(BACKEND_SERVICE_NAME, result.get(1).getStringParameter(GcpLoadBalancerMetadataView.LOADBALANCER_NAME));

        assertEquals(LoadBalancerType.PUBLIC, result.get(0).getType());
        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getBackendServiceParam(8080)));
        assertEquals(INSTANCE_GROUP_NAME, result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getInstanceGroupParam(8080)));

        assertEquals(BACKEND_SERVICE_NAME, result.get(1).getStringParameter(GcpLoadBalancerMetadataView.getBackendServiceParam(8080)));
        assertEquals(INSTANCE_GROUP_NAME, result.get(1).getStringParameter(GcpLoadBalancerMetadataView.getInstanceGroupParam(8080)));

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
        assertEquals(BACKEND_SERVICE_NAME, publicResult.get().getName());
        assertEquals(BACKEND_SERVICE_NAME, publicResult.get().getStringParameter(GcpLoadBalancerMetadataView.LOADBALANCER_NAME));


        assertEquals(PUBLIC_IP, publicResult.get().getIp());
        assertEquals(BACKEND_SERVICE_NAME, publicResult.get().getStringParameter(GcpLoadBalancerMetadataView.getBackendServiceParam(8080)));
        assertEquals(INSTANCE_GROUP_NAME, publicResult.get().getStringParameter(GcpLoadBalancerMetadataView.getInstanceGroupParam(8080)));


        Optional<CloudLoadBalancerMetadata> privateResult = result.stream().filter(p -> p.getType().equals(LoadBalancerType.PRIVATE)).findFirst();
        assertTrue(privateResult.isPresent());
        assertEquals(BACKEND_SERVICE_NAME, privateResult.get().getName());
        assertEquals(BACKEND_SERVICE_NAME, privateResult.get().getStringParameter(GcpLoadBalancerMetadataView.LOADBALANCER_NAME));
        assertEquals(PRIVATE_IP, privateResult.get().getIp());
        assertEquals(BACKEND_SERVICE_NAME, privateResult.get().getStringParameter(GcpLoadBalancerMetadataView.getBackendServiceParam(443)));
        assertEquals(INSTANCE_GROUP_NAME, privateResult.get().getStringParameter(GcpLoadBalancerMetadataView.getInstanceGroupParam(443)));


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
        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getName());
        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getStringParameter(GcpLoadBalancerMetadataView.LOADBALANCER_NAME));


        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getBackendServiceParam(443)));
        assertEquals(INSTANCE_GROUP_NAME, result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getInstanceGroupParam(443)));
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
        forwardingRule.setBackendService(BACKEND_SERVICE_NAME);
        when(forwardingRuleListResponse.getItems()).thenReturn(List.of(forwardingRule));

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        assertEquals(1, result.size());
        assertNull(result.get(0).getIp());
        assertEquals(LoadBalancerType.PUBLIC, result.get(0).getType());
        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getName());
        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getStringParameter(GcpLoadBalancerMetadataView.LOADBALANCER_NAME));

        assertNull(result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getBackendServiceParam(8080)));
        assertNull(result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getInstanceGroupParam(8080)));

    }

    @Test
    public void testNullPortInformation() {
        List<CloudResource> resources = new ArrayList<>();
        resources.add(createCloudResource(FORWARDING_RULE_NAME_1, ResourceType.GCP_FORWARDING_RULE));
        ForwardingRule publicFowardingRule = createPublicFowardingRule();
        publicFowardingRule.setPorts(null);
        when(forwardingRuleListResponse.getItems()).thenReturn(List.of(publicFowardingRule));

        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        assertEquals(1, result.size());
        assertEquals(PUBLIC_IP, result.get(0).getIp());
        assertEquals(LoadBalancerType.PUBLIC, result.get(0).getType());
        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getName());
        assertEquals(BACKEND_SERVICE_NAME, result.get(0).getStringParameter(GcpLoadBalancerMetadataView.LOADBALANCER_NAME));
        assertNull(result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getBackendServiceParam(8080)));
        assertNull(result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getInstanceGroupParam(8080)));
    }

    @Test
    public void testCollectLoadBalancerSkipsMetadataWhenRuntimeExceptionIsThrown() throws IOException {
        List<CloudResource> resources = new ArrayList<>();
        resources.add(createCloudResource(FORWARDING_RULE_NAME_1, ResourceType.GCP_FORWARDING_RULE));
        when(forwardingRulesList.execute()).thenThrow(new RuntimeException());


        List<CloudLoadBalancerMetadata> result = underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC), resources);

        assertEquals(0, result.size());
    }

    @Test
    void testCollectLoadBalancerMultiplePrivate() throws IOException {
        List<CloudResource> resources = new ArrayList<>();
        resources.add(createCloudResource(FORWARDING_RULE_NAME_1, ResourceType.GCP_FORWARDING_RULE));
        resources.add(createCloudResource(FORWARDING_RULE_NAME_2, ResourceType.GCP_FORWARDING_RULE));
        ForwardingRule fwr1 = new ForwardingRule();
        fwr1.setIPAddress("ipaddr1");
        fwr1.setName(FORWARDING_RULE_NAME_1);
        fwr1.setBackendService("backend");
        fwr1.setLoadBalancingScheme("INTERNAL");
        fwr1.setPorts(List.of("80", "81"));
        ForwardingRule fwr2 = new ForwardingRule();
        fwr2.setIPAddress("ipaddr1");
        fwr2.setName(FORWARDING_RULE_NAME_2);
        fwr2.setBackendService("backend");
        fwr2.setLoadBalancingScheme("INTERNAL");
        fwr2.setPorts(List.of("82", "83"));
        when(forwardingRuleListResponse.getItems()).thenReturn(List.of(fwr1, fwr2));
        Compute.RegionBackendServices regionBackendServices = mock(Compute.RegionBackendServices.class);
        when(compute.regionBackendServices()).thenReturn(regionBackendServices);
        Compute.RegionBackendServices.Get backendServiceGet = mock(Compute.RegionBackendServices.Get.class);
        BackendService backendService = new BackendService();
        backendService.setName("backend");
        backendService.setBackends(List.of(new Backend().setGroup("group")));
        when(backendServiceGet.execute()).thenReturn(backendService);
        when(regionBackendServices.get(any(), anyString(), anyString())).thenReturn(backendServiceGet);
        when(gcpLoadBalancerTypeConverter.getScheme("INTERNAL")).thenReturn(GcpLoadBalancerScheme.INTERNAL);

        List<CloudLoadBalancerMetadata> result =
                underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE), resources);

        assertThat(result).hasSize(1);
        assertEquals("ipaddr1", result.get(0).getIp());
        assertEquals(LoadBalancerType.PRIVATE, result.get(0).getType());
        assertEquals("backend", result.get(0).getName());
        assertEquals("backend", result.get(0).getStringParameter(GcpLoadBalancerMetadataView.LOADBALANCER_NAME));

        assertEquals("backend", result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getBackendServiceParam(80)));
        assertEquals("group", result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getInstanceGroupParam(80)));
        assertEquals("backend", result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getBackendServiceParam(82)));
        assertEquals("group", result.get(0).getStringParameter(GcpLoadBalancerMetadataView.getInstanceGroupParam(82)));
    }

    private ForwardingRule createPublicFowardingRule() {
        ForwardingRule forwardingRule = new ForwardingRule();
        forwardingRule.setLoadBalancingScheme("EXTERNAL");
        forwardingRule.setName(FORWARDING_RULE_NAME_1);
        forwardingRule.setIPAddress(PUBLIC_IP);
        forwardingRule.setPorts(List.of(PORT1));
        forwardingRule.setBackendService(BACKEND_SERVICE_NAME);
        return forwardingRule;
    }

    private ForwardingRule createPrivateFowardingRule() {
        ForwardingRule forwardingRule = new ForwardingRule();
        forwardingRule.setLoadBalancingScheme("INTERNAL");
        forwardingRule.setName(FORWARDING_RULE_NAME_2);
        forwardingRule.setIPAddress(PRIVATE_IP);
        forwardingRule.setPorts(List.of(PORT2));
        forwardingRule.setBackendService(BACKEND_SERVICE_NAME);
        return forwardingRule;
    }

    private BackendService createBackendService() {
        BackendService backendService = new BackendService();
        backendService.setName(BACKEND_SERVICE_NAME);
        Backend backend = new Backend();
        backend.setGroup(INSTANCE_GROUP_NAME);
        backendService.setBackends(List.of(backend));
        return backendService;
    }

    static CloudResource createCloudResource(String name, ResourceType type) {
        return CloudResource.builder()
                .withName(name)
                .withType(type)
                .withStatus(CommonStatus.CREATED)
                .withParameters(Collections.emptyMap())
                .build();
    }

}
