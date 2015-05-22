package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.GcpCredential;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class GcpImageCheckerStatus extends StackBasedStatusCheckerTask<GcpImageReadyPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpImageCheckerStatus.class);
    private static final String READY = "READY";

    @Override
    public boolean checkStatus(GcpImageReadyPollerObject gcpImageReadyPollerObject) {
        LOGGER.info("Checking status of Gcp Image '{}'.", gcpImageReadyPollerObject.getName());
        GcpCredential credential = (GcpCredential) gcpImageReadyPollerObject.getStack().getCredential();
        try {
            Compute.Images.Get getImages = gcpImageReadyPollerObject
                    .getCompute().images()
                    .get(credential.getProjectId(), gcpImageReadyPollerObject.getName());
            String status = getImages.execute().getStatus();
            return READY.equals(status) ? true : false;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void handleTimeout(GcpImageReadyPollerObject gcpImageReadyPollerObject) {
        throw new GcpResourceException(String.format(
                "Could not set up Gcp image '%s'; operation timed out.",
                gcpImageReadyPollerObject.getName()));
    }

    @Override
    public String successMessage(GcpImageReadyPollerObject gcpImageReadyPollerObject) {
        return String.format("Gcp image '%s' is ready on '%s' stack",
                gcpImageReadyPollerObject.getName(), gcpImageReadyPollerObject.getStack().getId());
    }

}
