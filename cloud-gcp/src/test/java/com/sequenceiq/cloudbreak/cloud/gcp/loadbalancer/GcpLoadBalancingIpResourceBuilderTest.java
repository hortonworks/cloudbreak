package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerScheme.EXTERNAL;
import static com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerScheme.INTERNAL;
import static com.sequenceiq.cloudbreak.cloud.service.CloudbreakResourceNameService.DELIMITER;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Address;
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
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpLoadBalancingIpResourceBuilderTest {

    private static final String STACK_NAME = "test-stack";

    private static final String REGION_NAME = "us-west-1";

    private static final String IP_ADDRESS_NAME = "test-stack-ip";

    private static final Long STACK_ID = 1L;

    private static final String PROJECT_ID = "project-id";

    private static final Integer HC_PORT = 8080;

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
    private Compute compute;

    @Mock
    private Compute.Addresses addresses;

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

    @Mock
    private ResourceNotifier resourceNotifier;

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
        lenient().when(gcpContext.getLocation()).thenReturn(location);
        lenient().when(gcpContext.getCompute()).thenReturn(compute);
        lenient().when(compute.addresses()).thenReturn(addresses);
        lenient().when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        lenient().when(cloudContext.getLocation()).thenReturn(location);
        lenient().when(cloudContext.getName()).thenReturn(STACK_NAME);
        lenient().when(location.getRegion()).thenReturn(region);
        lenient().when(region.getRegionName()).thenReturn(REGION_NAME);
        lenient().when(cloudContext.getId()).thenReturn(STACK_ID);
        lenient().when(region.getValue()).thenReturn(REGION_NAME);
        lenient().when(region.value()).thenReturn(REGION_NAME);

        cloudStack = CloudStack.builder().network(network).build();
    }

    private Group mockGroup(String name) {
        return Group.builder().withName(name).build();
    }

    @Test
    void testCreateWhenPublicLB() {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(Map.of(new TargetGroupPortPair(80, HC_PORT), Set.of(mockGroup("master"))));
        when(resourceRetriever.findAllByStatusAndTypeAndStack(eq(CREATED), any(), any())).thenReturn(new ArrayList<>());

        List<CloudResource> resources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, network);

        assertEquals(1, resources.size());
        assertEquals(ResourceType.GCP_RESERVED_IP, resources.get(0).getType());
        assertTrue(resources.get(0).getName().contains("p-8080"));
        assertEquals(CREATED, resources.get(0).getStatus());
    }

    @Test
    void testCreateWhenPrivateLB() {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(Map.of(new TargetGroupPortPair(80, HC_PORT), Set.of(mockGroup("master"))));
        when(resourceRetriever.findAllByStatusAndTypeAndStack(eq(CREATED), any(), any())).thenReturn(new ArrayList<>());

        List<CloudResource> resources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, network);

        assertEquals(1, resources.size());
        assertEquals(ResourceType.GCP_RESERVED_IP, resources.get(0).getType());
        assertTrue(resources.get(0).getName().contains("p-8080"));
        assertEquals(CREATED, resources.get(0).getStatus());
    }

    @Test
    void testCreateWhenResourceAlreadyExistsWithRightAttribute() {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(Map.of(new TargetGroupPortPair(80, HC_PORT), Set.of(mockGroup("master"))));
        CloudResource existingIp = CloudResource.builder()
                .withType(ResourceType.GCP_RESERVED_IP)
                .withStatus(CREATED)
                .withName("existing-p-8080-ip")
                .withParameters(Map.of(CloudResource.ATTRIBUTES, Enum.valueOf(LoadBalancerTypeAttribute.class, LoadBalancerType.PUBLIC.name()).asMap()))
                .build();
        when(resourceRetriever.findAllByStatusAndTypeAndStack(eq(CREATED), eq(ResourceType.GCP_RESERVED_IP), any()))
                .thenReturn(new ArrayList<>(List.of(existingIp)));

        List<CloudResource> resources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, network);

        assertEquals(existingIp.getName(), resources.getFirst().getName());
        assertEquals(existingIp.getType(), resources.getFirst().getType());
        assertEquals(existingIp.getStatus(), resources.getFirst().getStatus());
        assertEquals(HC_PORT, resources.getFirst().getParameter("hcport", Integer.class));
    }

    @Test
    void testCreateWhenResourceAlreadyExistsWithWrongAttribute() {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(Map.of(new TargetGroupPortPair(80, HC_PORT), Set.of(mockGroup("master"))));
        CloudResource existingIp = CloudResource.builder()
                .withType(ResourceType.GCP_RESERVED_IP)
                .withStatus(CREATED)
                .withName("existing-p-8080-ip")
                .withParameters(Map.of(CloudResource.ATTRIBUTES, Enum.valueOf(LoadBalancerTypeAttribute.class, LoadBalancerType.PRIVATE.name()).asMap()))
                .build();
        when(resourceRetriever.findAllByStatusAndTypeAndStack(eq(CREATED), eq(ResourceType.GCP_RESERVED_IP), any()))
                .thenReturn(new ArrayList<>(List.of(existingIp)));

        List<CloudResource> resources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, network);

        assertEquals(1, resources.size());
        CloudResource resource = resources.getFirst();
        assertNotEquals(existingIp, resource);
        assertEquals(ResourceType.GCP_RESERVED_IP, resource.getType());
        assertEquals(CREATED, resource.getStatus());
        assertTrue(resource.getName().contains("-p-8080-"));
        assertTrue(resource.isPersistent());
        assertEquals(LoadBalancerType.PUBLIC.name(), resource.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class).getName());
    }

    @Test
    void testCreateWhenResourceAlreadyExistsWithoutAttributeExistsOnGcp() throws IOException {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(Map.of(new TargetGroupPortPair(80, HC_PORT), Set.of(mockGroup("master"))));
        CloudResource existingIp1 = CloudResource.builder()
                .withType(ResourceType.GCP_RESERVED_IP)
                .withStatus(CREATED)
                .withName("existing-p-8080-ip-1")
                .build();
        CloudResource existingIp2 = CloudResource.builder()
                .withType(ResourceType.GCP_RESERVED_IP)
                .withStatus(CREATED)
                .withName("existing-p-8080-ip-2")
                .build();
        when(resourceRetriever.findAllByStatusAndTypeAndStack(eq(CREATED), eq(ResourceType.GCP_RESERVED_IP), any()))
                .thenReturn(new ArrayList<>(List.of(existingIp1, existingIp2)));
        Compute.Addresses.Get ip1Get = mock(Compute.Addresses.Get.class);
        when(ip1Get.execute()).thenReturn(new Address().setAddressType("EXTERNAL"));
        Compute.Addresses.Get ip2Get = mock(Compute.Addresses.Get.class);
        when(ip2Get.execute()).thenReturn(null);
        when(addresses.get(PROJECT_ID, REGION_NAME, existingIp1.getName())).thenReturn(ip1Get);
        when(addresses.get(PROJECT_ID, REGION_NAME, existingIp2.getName())).thenReturn(ip2Get);
        when(gcpLoadBalancerTypeConverter.getScheme(any(CloudLoadBalancer.class))).thenCallRealMethod();

        List<CloudResource> resources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, network);

        assertEquals(1, resources.size());
        CloudResource resource = resources.getFirst();
        assertNotEquals(existingIp1, resource);
        assertEquals(ResourceType.GCP_RESERVED_IP, resource.getType());
        assertEquals(CREATED, resource.getStatus());
        assertEquals(existingIp1.getName(), resource.getName());
        assertTrue(resource.isPersistent());
        assertEquals(LoadBalancerType.PUBLIC.name(), resource.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class).getName());
        verify(resourceNotifier).notifyUpdates(List.of(resource), cloudContext);
    }

    @Test
    void testCreateWhenResourceAlreadyExistsWithoutAttributeMissingOnGcp() throws IOException {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(Map.of(new TargetGroupPortPair(80, HC_PORT), Set.of(mockGroup("master"))));
        CloudResource existingIp1 = CloudResource.builder()
                .withType(ResourceType.GCP_RESERVED_IP)
                .withStatus(CREATED)
                .withName("existing-p-8080-ip-1")
                .build();
        CloudResource existingIp2 = CloudResource.builder()
                .withType(ResourceType.GCP_RESERVED_IP)
                .withStatus(CREATED)
                .withName("existing-p-8080-ip-2")
                .build();
        when(resourceRetriever.findAllByStatusAndTypeAndStack(eq(CREATED), eq(ResourceType.GCP_RESERVED_IP), any()))
                .thenReturn(new ArrayList<>(List.of(existingIp1, existingIp2)));
        Compute.Addresses.Get ip1Get = mock(Compute.Addresses.Get.class);
        when(ip1Get.execute()).thenReturn(new Address().setAddressType("INTERNAL"));
        Compute.Addresses.Get ip2Get = mock(Compute.Addresses.Get.class);
        when(ip2Get.execute()).thenReturn(null);
        when(addresses.get(PROJECT_ID, REGION_NAME, existingIp1.getName())).thenReturn(ip1Get);
        when(addresses.get(PROJECT_ID, REGION_NAME, existingIp2.getName())).thenReturn(ip2Get);
        when(gcpLoadBalancerTypeConverter.getScheme(any(CloudLoadBalancer.class))).thenCallRealMethod();

        List<CloudResource> resources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, network);

        assertEquals(1, resources.size());
        CloudResource resource = resources.getFirst();
        assertNotEquals(existingIp1, resource);
        assertEquals(ResourceType.GCP_RESERVED_IP, resource.getType());
        assertEquals(CREATED, resource.getStatus());
        assertEquals(existingIp2.getName(), resource.getName());
        assertTrue(resource.isPersistent());
        assertEquals(LoadBalancerType.PUBLIC.name(), resource.getParameter(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.class).getName());
        Map<String, Object> map = new HashMap<>(Enum.valueOf(LoadBalancerTypeAttribute.class, LoadBalancerType.PRIVATE.name()).asMap());
        map.put("hcport", HC_PORT);
        CloudResource expectedToUpdated = CloudResource.builder().cloudResource(existingIp1)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, map,
                                "hcport", HC_PORT))
                .build();
        verify(resourceNotifier).notifyUpdates(List.of(expectedToUpdated), cloudContext);
    }

    @Test
    void testCreateWhenResourceAlreadyExistsForInstance() {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        when(cloudLoadBalancer.getPortToTargetGroupMapping()).thenReturn(Map.of(new TargetGroupPortPair(80, HC_PORT), Set.of(mockGroup("master"))));
        CloudResource existingIp = CloudResource.builder().withType(ResourceType.GCP_RESERVED_IP).withStatus(CREATED).withName("existing-m-8080-ip").build();
        when(resourceRetriever.findAllByStatusAndTypeAndStack(eq(CREATED), eq(ResourceType.GCP_RESERVED_IP), any()))
                .thenReturn(new ArrayList<>(List.of(existingIp)));

        List<CloudResource> resources = underTest.create(gcpContext, authenticatedContext, cloudLoadBalancer, network);

        assertEquals(1, resources.size());
        assertEquals(ResourceType.GCP_RESERVED_IP, resources.get(0).getType());
        assertTrue(resources.get(0).getName().contains("p-8080"));
        assertEquals(CREATED, resources.get(0).getStatus());
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

    // New test methods for calculateLbTypeBasedOnGcpAddressType
    @Test
    void testCalculateLbTypeBasedOnGcpAddressTypeWhenGcpTypeMatchesAddressTypeAndLbIsPrivate() {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.PRIVATE);
        when(gcpLoadBalancerTypeConverter.getScheme(cloudLoadBalancer)).thenReturn(INTERNAL);
        Address addressFromProvider = new Address().setAddressType("INTERNAL");
        CloudResource resource = CloudResource.builder().withName("test-resource").withType(ResourceType.GCP_RESERVED_IP).build();

        LoadBalancerType result = ReflectionTestUtils.invokeMethod(underTest, "calculateLbTypeBasedOnGcpAddressType",
                cloudLoadBalancer, resource, addressFromProvider);

        assertEquals(LoadBalancerType.PRIVATE, result);
    }

    @Test
    void testCalculateLbTypeBasedOnGcpAddressTypeWhenGcpTypeMatchesAddressTypeAndLbIsGatewayPrivate() {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.GATEWAY_PRIVATE);
        when(gcpLoadBalancerTypeConverter.getScheme(cloudLoadBalancer)).thenReturn(INTERNAL);
        Address addressFromProvider = new Address().setAddressType("INTERNAL");
        CloudResource resource = CloudResource.builder().withName("test-resource").withType(ResourceType.GCP_RESERVED_IP).build();

        LoadBalancerType result = ReflectionTestUtils.invokeMethod(underTest, "calculateLbTypeBasedOnGcpAddressType",
                cloudLoadBalancer, resource, addressFromProvider);

        assertEquals(LoadBalancerType.GATEWAY_PRIVATE, result);
    }

    @Test
    void testCalculateLbTypeBasedOnGcpAddressTypeWhenAddressTypeIsInternalAndResourceNameContainsGatewayPrivatePart() {
        when(cloudLoadBalancer.getType()).thenReturn(LoadBalancerType.GATEWAY_PRIVATE);
        when(gcpLoadBalancerTypeConverter.getScheme(cloudLoadBalancer)).thenReturn(INTERNAL);
        Address addressFromProvider = new Address().setAddressType("INTERNAL");
        String lbGwPrivPart = DELIMITER + "gp" + DELIMITER;
        CloudResource resource = CloudResource.builder().withName("test-resource" + lbGwPrivPart + "name").withType(ResourceType.GCP_RESERVED_IP).build();

        LoadBalancerType result = ReflectionTestUtils.invokeMethod(underTest, "calculateLbTypeBasedOnGcpAddressType",
                cloudLoadBalancer, resource, addressFromProvider);

        assertEquals(LoadBalancerType.GATEWAY_PRIVATE, result);
    }

    @Test
    void testCalculateLbTypeBasedOnGcpAddressTypeWhenAddressTypeIsInternalAndResourceNameDoesNotContainGatewayPrivatePart() {
        when(gcpLoadBalancerTypeConverter.getScheme(cloudLoadBalancer)).thenReturn(EXTERNAL);
        Address addressFromProvider = new Address().setAddressType("INTERNAL");
        CloudResource resource = CloudResource.builder().withName("test-resource-name").withType(ResourceType.GCP_RESERVED_IP).build();

        LoadBalancerType result = ReflectionTestUtils.invokeMethod(underTest, "calculateLbTypeBasedOnGcpAddressType",
                cloudLoadBalancer, resource, addressFromProvider);

        assertEquals(LoadBalancerType.PRIVATE, result);
    }

    @Test
    void testCalculateLbTypeBasedOnGcpAddressTypeWhenAddressTypeIsNotInternalAndDoesNotMatchGcpType() {
        when(gcpLoadBalancerTypeConverter.getScheme(cloudLoadBalancer)).thenReturn(INTERNAL);
        Address addressFromProvider = new Address().setAddressType("EXTERNAL");
        CloudResource resource = CloudResource.builder().withName("test-resource-name").withType(ResourceType.GCP_RESERVED_IP).build();

        LoadBalancerType result = ReflectionTestUtils.invokeMethod(underTest, "calculateLbTypeBasedOnGcpAddressType",
                cloudLoadBalancer, resource, addressFromProvider);

        assertEquals(LoadBalancerType.PUBLIC, result);
    }

    @Test
    void testTryUpdateResourceWhenAddressNotFoundOnGcp() throws IOException {
        Compute.Addresses.Get ipGet = mock(Compute.Addresses.Get.class);
        when(addresses.get(PROJECT_ID, REGION_NAME, IP_ADDRESS_NAME)).thenReturn(ipGet);
        when(ipGet.execute()).thenReturn(null);
        when(gcpContext.getCompute()).thenReturn(compute);
        when(compute.addresses()).thenReturn(addresses);

        CloudResource resource = CloudResource.builder().withName(IP_ADDRESS_NAME).withType(ResourceType.GCP_RESERVED_IP).build();

        Optional<CloudResource> result = ReflectionTestUtils.invokeMethod(underTest, "tryUpdateResource",
                gcpContext, cloudLoadBalancer, HC_PORT, resource);

        assertFalse(result.isPresent());
    }

    @Test
    void testTryUpdateResourceWhenFetchFromProviderThrowsIOException() throws IOException {
        Compute.Addresses.Get ipGet = mock(Compute.Addresses.Get.class);
        when(addresses.get(PROJECT_ID, REGION_NAME, IP_ADDRESS_NAME)).thenReturn(ipGet);
        when(ipGet.execute()).thenThrow(new IOException("Network error"));
        when(gcpContext.getCompute()).thenReturn(compute);
        when(compute.addresses()).thenReturn(addresses);

        CloudResource resource = CloudResource.builder().withName(IP_ADDRESS_NAME).withType(ResourceType.GCP_RESERVED_IP).build();

        GcpResourceException thrown = assertThrows(GcpResourceException.class, () ->
                ReflectionTestUtils.invokeMethod(underTest, "tryUpdateResource",
                        gcpContext, cloudLoadBalancer, HC_PORT, resource));

        assertTrue(thrown.getMessage().contains("Failed to get address from provider for " + IP_ADDRESS_NAME));
    }
}
