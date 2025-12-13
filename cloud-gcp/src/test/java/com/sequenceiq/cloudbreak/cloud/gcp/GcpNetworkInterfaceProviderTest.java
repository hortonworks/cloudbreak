package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

    @InjectMocks
    private GcpNetworkInterfaceProvider underTest;

    @Mock
    private GcpInstanceProvider gcpInstanceProvider;

    @Mock
    private AuthenticatedContext authenticatedContext;

    static Object [] [] dataForInstances() {
        return new Object[] [] {
                {Map.of("instance1", true, "instance2", true, "instance3", true)},
                {Map.of("instance1", true, "instance2", true, "instance3", false)},
                {Map.of("instance1", false, "instance2", false, "instance3", false)},
                {Map.of()}
        };
    }

    @ParameterizedTest(name = "testProvideShouldReturnTheNetworkInterfaces{index}")
    @MethodSource("dataForInstances")
    public void testProvideShouldReturnTheNetworkInterfaces(Map<String, Boolean> instances) {
        List<String> instancesOnCloudProvider = instances.entrySet().stream().filter(Map.Entry::getValue).map(entry -> entry.getKey()).
        collect(Collectors.toList());
        List<Instance> gcpInstances = instancesOnCloudProvider.stream().
                map(instanceOnCloudProvider -> createInstance(instanceOnCloudProvider, instanceOnCloudProvider + "-az"))
                .collect(Collectors.toList());
        List<CloudResource> cloudResources = instances.entrySet().stream().map(entry ->
                        createCloudResource(entry.getKey(), entry.getKey() + "-az"))
                .collect(Collectors.toList());
        instances.entrySet().stream().forEach(entry -> {
            when(gcpInstanceProvider.getInstance(authenticatedContext, entry.getKey(), entry.getKey() + "-az"))
                    .thenReturn(gcpInstances.stream().filter(gcpInstance -> gcpInstance.getName().equals(entry.getKey())).findFirst());
        });

        Map<String, Optional<NetworkInterface>> actual = underTest.provide(authenticatedContext, cloudResources);

        assertEquals(instances.size(), actual.size());
        instances.entrySet().stream().forEach(entry -> {
            assertEquals(getNetworkForInstance(gcpInstances, entry.getKey()), actual.get(entry.getKey()));
            assertEquals(entry.getValue(), actual.get(entry.getKey()).isPresent());
        });
    }

    private CloudResource createCloudResource(String name, String availabilityZone) {
        return CloudResource.builder()
                .withName(name)
                .withType(ResourceType.GCP_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(Collections.emptyMap())
                .withAvailabilityZone(availabilityZone)
                .build();
    }

    private Instance createInstance(String name, String availabilityZone) {
        Instance instance = new Instance();
        instance.setName(name);
        instance.setNetworkInterfaces(List.of(new NetworkInterface()));
        instance.setZone(availabilityZone);
        return instance;
    }

    private Optional<NetworkInterface> getNetworkForInstance(List<Instance> gcpInstances, String instanceName) {
        return gcpInstances.stream()
                .filter(instance -> instance.getName().equals(instanceName))
                .findFirst()
                .map(gcpInstance -> gcpInstance.getNetworkInterfaces().get(0));
    }
}
