package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.AddInstancesFailedException;

@Component
public class GccDiskCheckerStatus implements StatusCheckerTask<GccDiskReadyPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccDiskCheckerStatus.class);
    private static final String READY = "READY";

    @Override
    public boolean checkStatus(GccDiskReadyPollerObject gccDiskReadyPollerObject) {
        LOGGER.info("Checking status of Gcc Disk '{}' on '{}' stack.",
                gccDiskReadyPollerObject.getName(),
                gccDiskReadyPollerObject.getStack().getId());
        GccTemplate gccTemplate = (GccTemplate) gccDiskReadyPollerObject.getStack().getTemplate();
        try {
            Compute.Disks.Get getDisks = gccDiskReadyPollerObject
                    .getCompute()
                    .disks()
                    .get(gccTemplate.getProjectId(), gccTemplate.getGccZone().getValue(), gccDiskReadyPollerObject.getName());
            String status = getDisks.execute().getStatus();
            return READY.equals(status) ? true : false;
        } catch (NullPointerException | IOException e) {
            return false;
        }
    }

    @Override
    public void handleTimeout(GccDiskReadyPollerObject gccDiskReadyPollerObject) {
        throw new AddInstancesFailedException(String.format(
                "Something went wrong. Instances in Gcc clouddisk '%s' not started in a reasonable timeframe on '%s' stack.",
                gccDiskReadyPollerObject.getName(), gccDiskReadyPollerObject.getStack().getId()));
    }

    @Override
    public String successMessage(GccDiskReadyPollerObject gccDiskReadyPollerObject) {
        return String.format("Gcc disk '%s' is ready on '%s' stack",
                gccDiskReadyPollerObject.getName(), gccDiskReadyPollerObject.getStack().getId());
    }
}
