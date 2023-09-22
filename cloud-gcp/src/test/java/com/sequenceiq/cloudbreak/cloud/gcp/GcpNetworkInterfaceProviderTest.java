package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.NetworkInterface;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class GcpNetworkInterfaceProviderTest {

    private static final String INSTANCE_NAME_PREFIX = "testcluster";

    private static final String INSTANCE_NAME_1 = "testcluster-w-1";

    private static final String INSTANCE_NAME_2 = "testcluster-w-2";

    private static final String INSTANCE_NAME_3 = "testcluster-w-3";

    @InjectMocks
    private GcpNetworkInterfaceProvider underTest;

    @Mock
    private GcpInstanceProvider gcpInstanceProvider;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Test
    public void testProvideShouldReturnsTheNetworkInterfaces() {
        List<CloudResource> instances = createCloudResources();
        when(gcpInstanceProvider.getInstanceNamePrefix(eq(instances))).thenReturn(INSTANCE_NAME_PREFIX);
        List<Instance> gcpInstances = List.of(createInstance(INSTANCE_NAME_1), createInstance(INSTANCE_NAME_2), createInstance(INSTANCE_NAME_3));
        when(gcpInstanceProvider.getInstances(eq(authenticatedContext), eq(INSTANCE_NAME_PREFIX))).thenReturn(gcpInstances);

        Map<String, Optional<NetworkInterface>> actual = underTest.provide(authenticatedContext, instances);

        assertEquals(3, actual.size());
        assertEquals(getNetworkForInstance(gcpInstances, INSTANCE_NAME_1), actual.get(INSTANCE_NAME_1));
        assertEquals(getNetworkForInstance(gcpInstances, INSTANCE_NAME_2), actual.get(INSTANCE_NAME_2));
        assertEquals(getNetworkForInstance(gcpInstances, INSTANCE_NAME_3), actual.get(INSTANCE_NAME_3));
    }

    @Test
    public void testProvideShouldReturnsMapWithoutNetworkWhenTheThereAreMissingNodes() {
        List<CloudResource> instances = createCloudResources();
        when(gcpInstanceProvider.getInstanceNamePrefix(eq(instances))).thenReturn(INSTANCE_NAME_PREFIX);
        List<Instance> gcpInstances = createGcpInstancesWithMissingNode();
        when(gcpInstanceProvider.getInstances(eq(authenticatedContext), eq(INSTANCE_NAME_PREFIX))).thenReturn(gcpInstances);

        Map<String, Optional<NetworkInterface>> actual = underTest.provide(authenticatedContext, instances);

        assertEquals(3, actual.size());
        assertEquals(getNetworkForInstance(gcpInstances, INSTANCE_NAME_1), actual.get(INSTANCE_NAME_1));
        assertEquals(getNetworkForInstance(gcpInstances, INSTANCE_NAME_2), actual.get(INSTANCE_NAME_2));
        assertEquals(getNetworkForInstance(gcpInstances, INSTANCE_NAME_3), actual.get(INSTANCE_NAME_3));
    }

    @Test
    public void testProvideShouldReturnsMapWithoutNetworkWhenTheThereAreNoResponseFromGcp() {
        List<CloudResource> instances = createCloudResources();
        when(gcpInstanceProvider.getInstanceNamePrefix(eq(instances))).thenReturn(INSTANCE_NAME_PREFIX);
        when(gcpInstanceProvider.getInstances(eq(authenticatedContext), eq(INSTANCE_NAME_PREFIX))).thenReturn(List.of());

        Map<String, Optional<NetworkInterface>> actual = underTest.provide(authenticatedContext, instances);

        assertEquals(3, actual.size());
        assertFalse(actual.get(INSTANCE_NAME_1).isPresent());
        assertFalse(actual.get(INSTANCE_NAME_2).isPresent());
        assertFalse(actual.get(INSTANCE_NAME_3).isPresent());
    }

    private List<CloudResource> createCloudResources() {
        return List.of(
                createCloudResource(INSTANCE_NAME_1),
                createCloudResource(INSTANCE_NAME_2),
                createCloudResource(INSTANCE_NAME_3));
    }

    private CloudResource createCloudResource(String name) {
        return CloudResource.builder()
                .withName(name)
                .withType(ResourceType.GCP_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(Collections.emptyMap())
                .build();
    }

    private List<Instance> createGcpInstancesWithMissingNode() {
        return List.of(createInstance(INSTANCE_NAME_1), createInstance(INSTANCE_NAME_3));
    }

    private Instance createInstance(String name) {
        Instance instance = new Instance();
        instance.setName(name);
        instance.setNetworkInterfaces(List.of(new NetworkInterface()));
        return instance;
    }

    private Optional<NetworkInterface> getNetworkForInstance(List<Instance> gcpInstances, String instanceName) {
        return gcpInstances.stream()
                .filter(instance -> instance.getName().equals(instanceName))
                .findFirst()
                .map(gcpInstance -> gcpInstance.getNetworkInterfaces().get(0));
    }
}
