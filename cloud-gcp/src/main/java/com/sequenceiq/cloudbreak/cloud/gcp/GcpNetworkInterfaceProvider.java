package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.NetworkInterface;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Component
public class GcpNetworkInterfaceProvider {

    @Inject
    private GcpInstanceProvider gcpInstanceProvider;

    Map<String, Optional<NetworkInterface>> provide(AuthenticatedContext authenticatedContext, List<CloudResource> instances) {
        String instanceNamePrefix = gcpInstanceProvider.getInstanceNamePrefix(instances);
        List<Instance> gcpInstances = gcpInstanceProvider.getInstances(authenticatedContext, instanceNamePrefix);
        return getNetworkMap(gcpInstances, instances);
    }

    private Map<String, Optional<NetworkInterface>> getNetworkMap(List<Instance> gcpInstances, List<CloudResource> instances) {
        return instances.stream().collect(Collectors.toMap(CloudResource::getName, getOptionalNetworkInterfaces(gcpInstances)));
    }

    private Function<CloudResource, Optional<NetworkInterface>> getOptionalNetworkInterfaces(List<Instance> gcpInstances) {
        return instance -> gcpInstances.stream()
                .filter(gcpInstance -> instance.getName().equals(gcpInstance.getName()))
                .findFirst()
                .map(gcpInstance -> gcpInstance.getNetworkInterfaces().get(0));
    }
}
