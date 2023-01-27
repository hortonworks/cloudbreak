package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.ForwardingRule;
import com.google.api.services.compute.model.ForwardingRuleList;
import com.google.api.services.compute.model.NetworkInterface;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer.GcpLoadBalancerTypeConverter;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpMetadataCollectorTest {

    private static final String AZ = "europe-north1";

    private static final String INSTANCE_NAME_1 = "testcluster-w-1";

    private static final String INSTANCE_NAME_2 = "testcluster-w-2";

    private static final String INSTANCE_NAME_3 = "testcluster-w-3";

    private static final String INSTANCE_NAME_4 = "testcluster-w-4";

    private static final String INSTANCE_NAME_5 = "testcluster-w-5";

    private static final String INSTANCE_NAME_6 = "testcluster-w-6";

    private static final String NETWORK_NAME = "testcluster-w-disc";

    private static final String DISC_NAME = "testcluster-w-network";

    private static final String PUBLIC_LB_NAME = "publicLb";

    private static final String PRIVATE_LB_NAME = "privateLb";

    private static final String GATEWAY_PRIVATE_LB_NAME = "gwPrivateLb";

    private static final List<CloudInstance> KNOWN_INSTANCES = Collections.emptyList();

    private static final String PUBLIC_IP = "192.168.1.1";

    private static final String PRIVATE_IP = "10.10.1.1";

    private static final String PROJECT_ID = "projectId";

    private static final String REGION = "region";

    @InjectMocks
    private GcpMetadataCollector underTest;

    @Mock
    private GcpNetworkInterfaceProvider gcpNetworkInterfaceProvider;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Mock
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    private AuthenticatedContext authenticatedContext;

    private List<CloudResource> resources;

    @BeforeEach
    void before() {
        authenticatedContext = createAuthenticatedContext();
        resources = createCloudResources();
        lenient().when(gcpStackUtil.getPrivateId(anyString())).thenReturn(1L);
        lenient().when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn(PROJECT_ID);
    }

    @Test
    void testCollectShouldReturnsWithTheVmMetadataListWithoutError() {
        List<CloudInstance> vms = createVms();
        Map<String, Optional<NetworkInterface>> networkInterfaces = createNetworkInterfaces();
        when(gcpNetworkInterfaceProvider.provide(eq(authenticatedContext), any())).thenReturn(networkInterfaces);

        List<CloudVmMetaDataStatus> actual = underTest.collect(authenticatedContext, resources, vms, KNOWN_INSTANCES);

        CloudVmMetaDataStatus vm1 = getVm(INSTANCE_NAME_1, actual);
        assertEquals(InstanceStatus.CREATED, vm1.getCloudVmInstanceStatus().getStatus());
        assertEquals(PUBLIC_IP, vm1.getMetaData().getPublicIp());
        assertEquals(PRIVATE_IP, vm1.getMetaData().getPrivateIp());

        CloudVmMetaDataStatus vm2 = getVm(INSTANCE_NAME_2, actual);
        assertEquals(InstanceStatus.CREATED, vm2.getCloudVmInstanceStatus().getStatus());
        assertEquals(PUBLIC_IP, vm2.getMetaData().getPublicIp());
        assertEquals(PRIVATE_IP, vm2.getMetaData().getPrivateIp());

        CloudVmMetaDataStatus vm3 = getVm(INSTANCE_NAME_3, actual);
        assertEquals(InstanceStatus.CREATED, vm3.getCloudVmInstanceStatus().getStatus());
        assertEquals(PUBLIC_IP, vm3.getMetaData().getPublicIp());
        assertEquals(PRIVATE_IP, vm3.getMetaData().getPrivateIp());
    }

    @Test
    void testCollectShouldReturnsWithVmsInUnknownStatusWhenThereAreNoNetworkInterfaceFound() {
        List<CloudInstance> vms = createVms();
        Map<String, Optional<NetworkInterface>> networkInterfaces = createEmptyNetworkInterfaces();
        when(gcpNetworkInterfaceProvider.provide(eq(authenticatedContext), any())).thenReturn(networkInterfaces);

        List<CloudVmMetaDataStatus> actual = underTest.collect(authenticatedContext, resources, vms, KNOWN_INSTANCES);

        CloudVmMetaDataStatus vm1 = getVm(INSTANCE_NAME_1, actual);
        assertEquals(InstanceStatus.UNKNOWN, vm1.getCloudVmInstanceStatus().getStatus());
        assertNull(vm1.getMetaData().getPublicIp());
        assertNull(vm1.getMetaData().getPrivateIp());

        CloudVmMetaDataStatus vm2 = getVm(INSTANCE_NAME_2, actual);
        assertEquals(InstanceStatus.UNKNOWN, vm2.getCloudVmInstanceStatus().getStatus());
        assertNull(vm2.getMetaData().getPublicIp());
        assertNull(vm2.getMetaData().getPrivateIp());

        CloudVmMetaDataStatus vm3 = getVm(INSTANCE_NAME_3, actual);
        assertEquals(InstanceStatus.UNKNOWN, vm3.getCloudVmInstanceStatus().getStatus());
        assertNull(vm3.getMetaData().getPublicIp());
        assertNull(vm3.getMetaData().getPrivateIp());
    }

    @Test
    void testCollectShouldReturnsWithVmsInTerminatedStatusWhenThereAreMoVmFound() {
        List<CloudInstance> vms = createVmsWithOtherIds();
        Map<String, Optional<NetworkInterface>> networkInterfaces = createEmptyNetworkInterfaces();
        when(gcpNetworkInterfaceProvider.provide(eq(authenticatedContext), any())).thenReturn(networkInterfaces);

        List<CloudVmMetaDataStatus> actual = underTest.collect(authenticatedContext, resources, vms, KNOWN_INSTANCES);

        CloudVmMetaDataStatus vm1 = getVm(INSTANCE_NAME_4, actual);
        assertEquals(InstanceStatus.TERMINATED, vm1.getCloudVmInstanceStatus().getStatus());
        assertNull(vm1.getMetaData().getPublicIp());
        assertNull(vm1.getMetaData().getPrivateIp());

        CloudVmMetaDataStatus vm2 = getVm(INSTANCE_NAME_5, actual);
        assertEquals(InstanceStatus.TERMINATED, vm2.getCloudVmInstanceStatus().getStatus());
        assertNull(vm2.getMetaData().getPublicIp());
        assertNull(vm2.getMetaData().getPrivateIp());

        CloudVmMetaDataStatus vm3 = getVm(INSTANCE_NAME_6, actual);
        assertEquals(InstanceStatus.TERMINATED, vm3.getCloudVmInstanceStatus().getStatus());
        assertNull(vm3.getMetaData().getPublicIp());
        assertNull(vm3.getMetaData().getPrivateIp());
    }

    @Test
    void testCollectLoadBalancerPublicAndPrivate() throws IOException {
        Compute compute = mock(Compute.class);
        when(gcpComputeFactory.buildCompute(authenticatedContext.getCloudCredential())).thenReturn(compute);
        Compute.ForwardingRules forwardingRules = mock(Compute.ForwardingRules.class);
        when(compute.forwardingRules()).thenReturn(forwardingRules);
        Compute.ForwardingRules.List forwardingRulesList = mock(Compute.ForwardingRules.List.class);
        when(forwardingRules.list(PROJECT_ID, REGION)).thenReturn(forwardingRulesList);
        ForwardingRuleList fwList = new ForwardingRuleList();
        ForwardingRule fwr1 = new ForwardingRule();
        fwr1.setIPAddress("ipaddr1");
        fwr1.setName(PRIVATE_LB_NAME);
        fwr1.setLoadBalancingScheme("INTERNAL");
        ForwardingRule fwr2 = new ForwardingRule();
        fwr2.setIPAddress("ipaddr2");
        fwr2.setName(PUBLIC_LB_NAME);
        fwr2.setLoadBalancingScheme("EXTERNAL");
        fwList.setItems(List.of(fwr1, fwr2));
        when(forwardingRulesList.execute()).thenReturn(fwList);
        when(gcpLoadBalancerTypeConverter.getScheme("INTERNAL")).thenReturn(GcpLoadBalancerScheme.INTERNAL);
        when(gcpLoadBalancerTypeConverter.getScheme("EXTERNAL")).thenReturn(GcpLoadBalancerScheme.EXTERNAL);

        List<CloudLoadBalancerMetadata> result =
                underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE), resources);

        assertThat(result).hasSize(2);
        assertThat(result).map(CloudLoadBalancerMetadata::getType).containsExactlyInAnyOrder(LoadBalancerType.PUBLIC, LoadBalancerType.PRIVATE);
    }

    @Test
    void testCollectLoadBalancerGatewayAndPrivate() throws IOException {
        Compute compute = mock(Compute.class);
        when(gcpComputeFactory.buildCompute(authenticatedContext.getCloudCredential())).thenReturn(compute);
        Compute.ForwardingRules forwardingRules = mock(Compute.ForwardingRules.class);
        when(compute.forwardingRules()).thenReturn(forwardingRules);
        Compute.ForwardingRules.List forwardingRulesList = mock(Compute.ForwardingRules.List.class);
        when(forwardingRules.list(PROJECT_ID, REGION)).thenReturn(forwardingRulesList);
        ForwardingRuleList fwList = new ForwardingRuleList();
        ForwardingRule fwr1 = new ForwardingRule();
        fwr1.setIPAddress("ipaddr1");
        fwr1.setName(PRIVATE_LB_NAME);
        fwr1.setLoadBalancingScheme("INTERNAL");
        ForwardingRule fwr2 = new ForwardingRule();
        fwr2.setIPAddress("ipaddr2");
        fwr2.setName(GATEWAY_PRIVATE_LB_NAME);
        fwr2.setLoadBalancingScheme("INTERNAL");
        fwList.setItems(List.of(fwr1, fwr2));
        when(forwardingRulesList.execute()).thenReturn(fwList);
        when(gcpLoadBalancerTypeConverter.getScheme("INTERNAL")).thenReturn(GcpLoadBalancerScheme.INTERNAL);

        List<CloudLoadBalancerMetadata> result =
                underTest.collectLoadBalancer(authenticatedContext, List.of(LoadBalancerType.GATEWAY_PRIVATE, LoadBalancerType.PRIVATE), resources);

        assertThat(result).hasSize(2);
        assertThat(result).map(CloudLoadBalancerMetadata::getType).containsExactlyInAnyOrder(LoadBalancerType.GATEWAY_PRIVATE, LoadBalancerType.PRIVATE);
    }

    private CloudVmMetaDataStatus getVm(String name, List<CloudVmMetaDataStatus> actual) {
        return actual.stream()
                .filter(vm -> vm.getCloudVmInstanceStatus().getCloudInstance().getInstanceId().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Instance not found"));
    }

    private List<CloudInstance> createVms() {
        return List.of(
                createCloudInstance(INSTANCE_NAME_1, 1L),
                createCloudInstance(INSTANCE_NAME_2, 2L),
                createCloudInstance(INSTANCE_NAME_3, 3L));
    }

    private List<CloudInstance> createVmsWithOtherIds() {
        return List.of(
                createCloudInstance(INSTANCE_NAME_4, 4L),
                createCloudInstance(INSTANCE_NAME_5, 5L),
                createCloudInstance(INSTANCE_NAME_6, 6L));
    }

    private CloudInstance createCloudInstance(String name, Long privateId) {
        InstanceTemplate instanceTemplate = createInstanceTemplate(privateId);
        return new CloudInstance(name, instanceTemplate, null, "subnet-1", "az1");
    }

    private InstanceTemplate createInstanceTemplate(Long privateId) {
        return new InstanceTemplate(null, null, privateId, Collections.emptyList(), null, null, null, null, TemporaryStorage.ATTACHED_VOLUMES, 0L);
    }

    private Map<String, Optional<NetworkInterface>> createNetworkInterfaces() {
        Optional<NetworkInterface> networkInterface = createNetworkInterface();
        return Map.of(
                INSTANCE_NAME_1, networkInterface,
                INSTANCE_NAME_2, networkInterface,
                INSTANCE_NAME_3, networkInterface);
    }

    private Map<String, Optional<NetworkInterface>> createEmptyNetworkInterfaces() {
        return Map.of(
                INSTANCE_NAME_1, Optional.empty(),
                INSTANCE_NAME_2, Optional.empty(),
                INSTANCE_NAME_3, Optional.empty());
    }

    private Optional<NetworkInterface> createNetworkInterface() {
        NetworkInterface networkInterface = new NetworkInterface();
        AccessConfig accessConfig = new AccessConfig();
        networkInterface.setNetworkIP(PRIVATE_IP);
        accessConfig.setNatIP(PUBLIC_IP);
        networkInterface.setAccessConfigs(List.of(accessConfig));
        return Optional.of(networkInterface);
    }

    private AuthenticatedContext createAuthenticatedContext() {
        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = new CloudCredential("1", "gcp-cred", Collections.singletonMap(PROJECT_ID, "gcp-cred"), "acc");
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }

    private CloudContext createCloudContext() {
        Location location = Location.location(Region.region(REGION), AvailabilityZone.availabilityZone(AZ));
        return CloudContext.Builder.builder()
                .withName("test-cluster")
                .withLocation(location)
                .withUserName("")
                .build();
    }

    private List<CloudResource> createCloudResources() {
        return List.of(
            createCloudResource(INSTANCE_NAME_1, ResourceType.GCP_INSTANCE),
            createCloudResource(INSTANCE_NAME_2, ResourceType.GCP_INSTANCE),
            createCloudResource(INSTANCE_NAME_3, ResourceType.GCP_INSTANCE),
            createCloudResource(DISC_NAME, ResourceType.GCP_ATTACHED_DISK),
            createCloudResource(NETWORK_NAME, ResourceType.GCP_NETWORK),
            createCloudResource(PUBLIC_LB_NAME, ResourceType.GCP_FORWARDING_RULE,
                    Map.of(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.PUBLIC)),
            createCloudResource(GATEWAY_PRIVATE_LB_NAME, ResourceType.GCP_FORWARDING_RULE,
                    Map.of(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.GATEWAY_PRIVATE)),
            createCloudResource(PRIVATE_LB_NAME, ResourceType.GCP_FORWARDING_RULE,
                    Map.of(CloudResource.ATTRIBUTES, LoadBalancerTypeAttribute.PRIVATE)));
    }

    private CloudResource createCloudResource(String name, ResourceType type) {
        return createCloudResource(name, type, Collections.emptyMap());
    }

    private CloudResource createCloudResource(String name, ResourceType type, Map<String, Object> params) {
        return CloudResource.builder()
                .withName(name)
                .withType(type)
                .withStatus(CommonStatus.CREATED)
                .withParameters(params)
                .build();
    }
}
