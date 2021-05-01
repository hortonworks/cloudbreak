package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;
import java.util.ArrayList;
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
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Component
public class GcpNetworkInterfaceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpNetworkInterfaceProvider.class);

    private static final String DELIMITER = "-";

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

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

    public List<Instance> getInstances(AuthenticatedContext authenticatedContext, String instanceNamePrefix) {
        List<Instance> instances = new ArrayList<>();
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String stackName = cloudContext.getName();
        LOGGER.debug(String.format("Collecting instances for stack: %s", stackName));
        long startTime = new Date().getTime();

        try {
            Compute.Instances.List request = getRequest(authenticatedContext, instanceNamePrefix);
            InstanceList response;
            do {
                response = request.execute();
                if (response.getItems() == null) {
                    continue;
                }
                instances.addAll(response.getItems());
                request.setPageToken(response.getNextPageToken());
            } while (response.getNextPageToken() != null);
        } catch (IOException e) {
            LOGGER.debug("Error during instance collection", e);
        }
        logResponse(instances, startTime, stackName);
        return instances;
    }

    private Compute.Instances.List getRequest(AuthenticatedContext authenticatedContext, String instanceNamePrefix) throws IOException {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        Compute compute = gcpComputeFactory.buildCompute(credential);
        return compute.instances()
                .list(gcpStackUtil.getProjectId(credential), authenticatedContext.getCloudContext().getLocation()
                        .getAvailabilityZone()
                        .value())
                .setFilter(String.format("name=%s-*", instanceNamePrefix));
    }

    private void logResponse(List<Instance> instanceList, long startTime, String stackName) {
        long endTime = new Date().getTime();
        if (instanceList != null) {
            LOGGER.debug(String.format("%d instance retrieved for stack %s during %dms", instanceList.size(), stackName, endTime - startTime));
        } else {
            LOGGER.debug(String.format("There are no instances found for stack %s", stackName));
        }
    }
}
