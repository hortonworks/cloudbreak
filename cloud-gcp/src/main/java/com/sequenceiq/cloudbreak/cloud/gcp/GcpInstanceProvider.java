package com.sequenceiq.cloudbreak.cloud.gcp;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class GcpInstanceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpInstanceProvider.class);

    @Inject
    private GcpComputeFactory gcpComputeFactory;

    @Inject
    private GcpStackUtil gcpStackUtil;

    public Optional<Instance> getInstance(AuthenticatedContext authenticatedContext, String instanceName, String zone) {
        CloudContext cloudContext = authenticatedContext.getCloudContext();
        String stackName = cloudContext.getName();
        LOGGER.debug("Collecting instances for stack: {}", stackName);
        long startTime = new Date().getTime();
        Instance instance = null;
        try {
            Compute.Instances.Get request = getRequest(authenticatedContext, zone, instanceName);
            instance = request.execute();
        } catch (IOException e) {
            LOGGER.debug("Error during instance collection", e);
        }
        logResponse(instance, startTime, stackName);
        return Optional.ofNullable(instance);
    }

    private Compute.Instances.Get getRequest(AuthenticatedContext authenticatedContext, String zone, String instanceName) throws IOException {
        CloudCredential credential = authenticatedContext.getCloudCredential();
        Compute compute = gcpComputeFactory.buildCompute(credential);
        return compute.instances()
                .get(gcpStackUtil.getProjectId(credential), zone, instanceName);
    }

    private void logResponse(Instance instance, long startTime, String stackName) {
        long endTime = new Date().getTime();
        if (instance != null) {
            LOGGER.debug("{} instance retrieved for stack {} during {}ms", instance, stackName, endTime - startTime);
        } else {
            LOGGER.debug("There are no instances found for stack {}", stackName);
        }
    }
}
