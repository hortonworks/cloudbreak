package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Component
public class GcpNetworkInterfaceProvider {

    @Inject
    private GcpInstanceProvider gcpInstanceProvider;

    Map<String, Optional<GcpNetworkAndInstanceMetadata>> provide(AuthenticatedContext authenticatedContext, List<CloudResource> instances) {
        List<Instance> gcpInstances = new ArrayList<>();
        for (CloudResource cloudResource : instances) {
            Optional<Instance> instanceOptional = gcpInstanceProvider.getInstance(authenticatedContext, cloudResource.getName(),
                    cloudResource.getAvailabilityZone());
            instanceOptional.ifPresent(instance -> gcpInstances.add(instance));
        }
        return getNetworkMap(gcpInstances, instances);
    }

    private Map<String, Optional<GcpNetworkAndInstanceMetadata>> getNetworkMap(List<Instance> gcpInstances, List<CloudResource> instances) {
        return instances.stream().collect(Collectors.toMap(CloudResource::getName, getOptionalNetworkInterfaces(gcpInstances)));
    }

    private Function<CloudResource, Optional<GcpNetworkAndInstanceMetadata>> getOptionalNetworkInterfaces(List<Instance> gcpInstances) {
        return instance -> gcpInstances.stream()
                .filter(gcpInstance -> instance.getName().equals(gcpInstance.getName()))
                .findFirst()
                .map(gcpInstance -> new GcpNetworkAndInstanceMetadata(gcpInstance.getNetworkInterfaces().get(0),
                        StringUtils.substringAfterLast(gcpInstance.getMachineType(), "/")));
    }
}
