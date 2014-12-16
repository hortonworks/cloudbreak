package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

@Component
public class GccResourceCheckerStatus implements StatusCheckerTask<GccResourceReadyPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccResourceCheckerStatus.class);
    private static final int FINISHED = 100;

    @Override
    public boolean checkStatus(GccResourceReadyPollerObject gccResourceReadyPollerObject) {
        MDCBuilder.buildMdcContext(gccResourceReadyPollerObject.getStack());
        LOGGER.info("Checking status of Gcc resource '{}'.", gccResourceReadyPollerObject.getName());
        try {
            Integer progress = gccResourceReadyPollerObject.getZoneOperations().execute().getProgress();
            return (progress.intValue() != FINISHED) ? false : true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void handleTimeout(GccResourceReadyPollerObject gccResourceReadyPollerObject) {
        throw new GccResourceCreationException(String.format(
                "Something went wrong. Instances in Gcc resource '%s' not started in a reasonable timeframe on '%s' stack.",
                gccResourceReadyPollerObject.getName(), gccResourceReadyPollerObject.getStack().getId()));
    }

    @Override
    public String successMessage(GccResourceReadyPollerObject gccResourceReadyPollerObject) {
        MDCBuilder.buildMdcContext(gccResourceReadyPollerObject.getStack());
        return String.format("Gcc resource '%s' is ready on '%s' stack",
                gccResourceReadyPollerObject.getName(), gccResourceReadyPollerObject.getStack().getId());
    }
}
