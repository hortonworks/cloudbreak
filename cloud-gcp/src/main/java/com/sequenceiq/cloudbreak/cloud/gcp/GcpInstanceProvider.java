package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceList;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Component
public class GcpInstanceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceProvider.class);

    private static final String DELIMITER = "-";

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    public List<Instance> getInstances(AuthenticatedContext authenticatedContext, String instanceNamePrefix) {
        List<Instance> instances = new ArrayList<>();
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String stackName = cloudContext.getName();
        LOGGER.debug("Collecting instances for stack: {}", stackName);
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

    public String getInstanceNamePrefix(List<CloudResource> instances) {
        return instances.get(0).getName().split(DELIMITER)[0];
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
            LOGGER.debug("{} instance retrieved for stack {} during {}ms", instanceList.size(), stackName, endTime - startTime);
        } else {
            LOGGER.debug("There are no instances found for stack {}", stackName);
        }
    }
}
