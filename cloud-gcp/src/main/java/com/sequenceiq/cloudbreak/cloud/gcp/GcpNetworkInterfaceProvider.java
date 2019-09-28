package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceList;
import com.google.api.services.compute.model.NetworkInterface;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpApiFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Component
class GcpNetworkInterfaceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpNetworkInterfaceProvider.class);

    private static final String DELIMITER = "-";

    @Inject
    private GcpApiFactory gcpApiFactory;

    Map<String, Optional<NetworkInterface>> provide(AuthenticatedContext authenticatedContext, List<CloudResource> instances) {
        String instanceNamePrefix = getInstanceNamePrefix(instances);
        List<Instance> gcpInstances = getInstances(authenticatedContext, instanceNamePrefix);
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

    private String getInstanceNamePrefix(List<CloudResource> instances) {
        return instances.get(0).getName().split(DELIMITER)[0];
    }

    private List<Instance> getInstances(AuthenticatedContext authenticatedContext, String instanceNamePrefix) {
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String stackName = cloudContext.getName();
        LOGGER.debug(String.format("Collecting instance metadata for stack: %s", stackName));
        long startTime = new Date().getTime();
        CloudCredential credential = authenticatedContext.getCloudCredential();
        Compute compute = gcpApiFactory.getComputeApi(credential);

        InstanceList instances = null;
        try {
            instances = compute.instances()
                    .list(GcpStackUtil.getProjectId(credential), cloudContext.getLocation().getAvailabilityZone().value())
                    .setFilter(String.format("name=%s-*", instanceNamePrefix))
                    .execute();
        } catch (IOException e) {
            LOGGER.debug("Error dunging metadata collection", e);
        }
        logResponse(instances, startTime, stackName);
        return Optional.ofNullable(instances).map(InstanceList::getItems).orElse(Collections.emptyList());
    }

    private void logResponse(InstanceList instanceList, long startTime, String stackName) {
        long endTime = new Date().getTime();
        if (instanceList != null && instanceList.getItems() != null) {
            LOGGER.debug(String.format("%d instance retrieved for stack %s during %dms", instanceList.getItems().size(), stackName, endTime - startTime));
        } else {
            LOGGER.debug(String.format("There are no instances found for stack %s", stackName));
        }
    }
}
