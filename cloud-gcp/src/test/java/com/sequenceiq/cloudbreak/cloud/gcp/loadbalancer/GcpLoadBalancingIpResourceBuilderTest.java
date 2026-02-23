package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerScheme.EXTERNAL;
import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerScheme.INTERNAL;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.compute.GcpReservedIpResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpLoadBalancingIpResourceBuilderTest {

    private static final String STACK_NAME = "test-stack";

    private static final String REGION_NAME = "us-west-1";

    private static final String IP_ADDRESS_NAME = "test-stack-ip";

    private static final Long STACK_ID = 1L;

    private static final String PROJECT_ID = "project-id";

    @InjectMocks
    private GcpLoadBalancingIpResourceBuilder underTest;

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

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private GcpReservedIpResourceBuilder reservedIpResourceBuilder;

    @Mock
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    private CloudStack cloudStack;

    @BeforeEach
    void setUp() {
        GcpResourceNameService gcpResourceNameService = new GcpResourceNameService();
        ReflectionTestUtils.setField(gcpResourceNameService, "maxResourceNameLength", 100);
        ReflectionTestUtils.setField(underTest, "resourceNameService", gcpResourceNameService);
        ReflectionTestUtils.setField(underTest, "resourceRetriever", resourceRetriever);
        ReflectionTestUtils.setField(underTest, "reservedIpResourceBuilder", reservedIpResourceBuilder);
        ReflectionTestUtils.setField(underTest, "gcpLoadBalancerTypeConverter", gcpLoadBalancerTypeConverter);

        lenient().when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
        lenient().when(gcpContext.getName()).thenReturn(STACK_NAME);
        lenient().when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        lenient().when(cloudContext.getLocation()).thenReturn(location);
        lenient().when(cloudContext.getName()).thenReturn(STACK_NAME);
        lenient().when(location.getRegion()).thenReturn(region);
        lenient().when(region.getRegionName()).thenReturn(REGION_NAME);
        lenient().when(cloudContext.getId()).thenReturn(STACK_ID);

        cloudStack = CloudStack.builder().network(network).build();
    }

    private Group mockGroup(String name) {
        return Group.builder().withName(name).build();
    }

    @Test
    void testCreateWhenPublicLB() {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(Map.of(new TargetGroupPortPair(80, 8080), Set.of(mockGroup("master"))));
        when(resourceRetriever.findByStatusAndTypeAndStack(any(), any(), any())).thenReturn(Optional.empty());

        List<CloudResource> resources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, network);

        assertEquals(1, resources.size());
        assertEquals(ResourceType.GCP_RESERVED_IP, resources.get(0).getType());
        assertTrue(resources.get(0).getName().contains("p-8080"));
        assertEquals(CREATED, resources.get(0).getStatus());
    }

    @Test
    void testCreateWhenPrivateLB() {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(Map.of(new TargetGroupPortPair(80, 8080), Set.of(mockGroup("master"))));
        when(resourceRetriever.findByStatusAndTypeAndStack(any(), any(), any())).thenReturn(Optional.empty());

        List<CloudResource> resources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, network);

        assertEquals(1, resources.size());
        assertEquals(ResourceType.GCP_RESERVED_IP, resources.get(0).getType());
        assertTrue(resources.get(0).getName().contains("p-8080"));
        assertEquals(CREATED, resources.get(0).getStatus());
    }

    @Test
    void testCreateWhenResourceAlreadyExists() {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(Map.of(new TargetGroupPortPair(80, 8080), Set.of(mockGroup("master"))));
        CloudResource existingIp = CloudResource.builder().withType(ResourceType.GCP_RESERVED_IP).withStatus(CREATED).withName("existing-ip").build();
        when(resourceRetriever.findByStatusAndTypeAndStack(any(), eq(ResourceType.GCP_RESERVED_IP), any())).thenReturn(Optional.of(existingIp));

        List<CloudResource> resources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, network);

        assertEquals(existingIp, resources.getFirst());
    }

    @Test
    void testBuildWhenPublicIp() throws Exception {
        CloudResource resource = CloudResource.builder().withType(ResourceType.GCP_RESERVED_IP).withName(IP_ADDRESS_NAME).build();
        when(gcpLoadBalancerTypeConverter.getScheme(cloudLoadBalancer)).thenReturn(EXTERNAL);
        when(reservedIpResourceBuilder.buildReservedIp(gcpContext, List.of(resource), cloudStack, EXTERNAL)).thenReturn(List.of(resource));

        List<CloudResource> resultList = underTest.build(gcpContext, authenticatedContext, List.of(resource), cloudLoadBalancer, cloudStack);

        assertNotNull(resultList);
        assertEquals(1, resultList.size());
        assertEquals(IP_ADDRESS_NAME, resultList.get(0).getName());
    }

    @Test
    void testBuildWhenPrivateIp() throws Exception {
        CloudResource resource = CloudResource.builder().withType(ResourceType.GCP_RESERVED_IP).withName(IP_ADDRESS_NAME).build();
        when(gcpLoadBalancerTypeConverter.getScheme(cloudLoadBalancer)).thenReturn(INTERNAL);
        when(reservedIpResourceBuilder.buildReservedIp(gcpContext, List.of(resource), cloudStack, INTERNAL)).thenReturn(List.of(resource));

        List<CloudResource> resultList = underTest.build(gcpContext, authenticatedContext, List.of(resource), cloudLoadBalancer, cloudStack);

        assertNotNull(resultList);
        assertEquals(1, resultList.size());

    }

    @Test
    void testBuildWhenApiThrowsException() throws Exception {
        CloudResource resource = CloudResource.builder().withType(ResourceType.GCP_RESERVED_IP).withName(IP_ADDRESS_NAME).build();
        when(gcpLoadBalancerTypeConverter.getScheme(cloudLoadBalancer)).thenReturn(EXTERNAL);
        when(reservedIpResourceBuilder.buildReservedIp(gcpContext, List.of(resource), cloudStack, EXTERNAL)).thenThrow(new GcpResourceException("API error"));

        assertThrows(GcpResourceException.class,
                () -> underTest.build(gcpContext, authenticatedContext, List.of(resource), cloudLoadBalancer, cloudStack));
    }

    @Test
    void testDelete() throws Exception {
        CloudResource resource = CloudResource.builder().withType(ResourceType.GCP_RESERVED_IP).withName(IP_ADDRESS_NAME).build();

        underTest.delete(gcpContext, authenticatedContext, resource);

        verify(reservedIpResourceBuilder).deleteReservedIP(gcpContext, resource);
    }

    @Test
    void testDeleteWhenApiThrowsOtherException() throws Exception {
        CloudResource resource = CloudResource.builder().withType(ResourceType.GCP_RESERVED_IP).withName(IP_ADDRESS_NAME).build();
        GoogleJsonResponseException badRequestException = new GoogleJsonResponseException(
                new HttpResponseException.Builder(400, "Bad Request", new HttpHeaders()), new GoogleJsonError());
        when(reservedIpResourceBuilder.deleteReservedIP(gcpContext, resource)).thenThrow(badRequestException);

        assertThrows(GoogleJsonResponseException.class, () -> underTest.delete(gcpContext, authenticatedContext, resource));
    }

    @Test
    void testResourceType() {
        assertEquals(ResourceType.GCP_RESERVED_IP, underTest.resourceType());
    }

    @Test
    void testOrder() {
        assertEquals(3, underTest.order());
    }
}