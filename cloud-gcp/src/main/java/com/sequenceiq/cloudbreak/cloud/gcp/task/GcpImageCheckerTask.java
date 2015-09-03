package com.sequenceiq.cloudbreak.cloud.gcp.task;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.BooleanStateConnector;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;

public class GcpImageCheckerTask implements BooleanStateConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpImageCheckerTask.class);
    private static final String READY = "READY";
    private String projectId;
    private String name;
    private Compute compute;

    public GcpImageCheckerTask(String projectId, String name, Compute compute) {
        this.projectId = projectId;
        this.name = name;
        this.compute = compute;
    }

    @Override
    public Boolean check(AuthenticatedContext authenticatedContext) {
        LOGGER.info("Checking status of Gcp image '{}' copy", name);
        try {
            Compute.Images.Get getImages = compute.images().get(projectId, name);
            String status = getImages.execute().getStatus();
            LOGGER.info("Status of image {} copy: {}", name, status);
            return READY.equals(status);
        } catch (IOException e) {
            LOGGER.warn("Failed to retrieve image copy status", e);
            return false;
        }
    }
}
