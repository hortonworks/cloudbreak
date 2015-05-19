package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.GccCredential;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class GccImageCheckerStatus extends StackBasedStatusCheckerTask<GccImageReadyPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccImageCheckerStatus.class);
    private static final String READY = "READY";

    @Override
    public boolean checkStatus(GccImageReadyPollerObject gccImageReadyPollerObject) {
        LOGGER.info("Checking status of Gcc Image '{}'.", gccImageReadyPollerObject.getName());
        GccCredential credential = (GccCredential) gccImageReadyPollerObject.getStack().getCredential();
        try {
            Compute.Images.Get getImages = gccImageReadyPollerObject
                    .getCompute().images()
                    .get(credential.getProjectId(), gccImageReadyPollerObject.getName());
            String status = getImages.execute().getStatus();
            return READY.equals(status) ? true : false;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void handleTimeout(GccImageReadyPollerObject gccImageReadyPollerObject) {
        throw new GcpResourceException(String.format(
                "Could not set up Gcp image '%s'; operation timed out.",
                gccImageReadyPollerObject.getName()));
    }

    @Override
    public String successMessage(GccImageReadyPollerObject gccImageReadyPollerObject) {
        return String.format("Gcc image '%s' is ready on '%s' stack",
                gccImageReadyPollerObject.getName(), gccImageReadyPollerObject.getStack().getId());
    }

}
