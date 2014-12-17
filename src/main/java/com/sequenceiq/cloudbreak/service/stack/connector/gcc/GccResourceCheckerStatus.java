package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.model.Operation;
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
        Operation execute = null;
        try {
            execute = gccResourceReadyPollerObject.getZoneOperations().execute();

            if (execute.getHttpErrorStatusCode() != null) {
                throw new GccResourceCreationException(String.format(
                        "Something went wrong. Resource in Gcc '%s' with '%s' operation failed on '%s' stack with %s message.",
                        gccResourceReadyPollerObject.getName(),
                        gccResourceReadyPollerObject.getOperationName(),
                        gccResourceReadyPollerObject.getStack().getId(),
                        execute.getHttpErrorMessage()));
            } else {
                Integer progress = execute.getProgress();
                return (progress.intValue() != FINISHED) ? false : true;
            }
        } catch (IOException e) {
            throw new GccResourceCreationException(String.format(
                    "Something went wrong. Resource in Gcc '%s' with '%s' operation failed on '%s' stack with %s message.",
                    gccResourceReadyPollerObject.getName(),
                    gccResourceReadyPollerObject.getOperationName(),
                    gccResourceReadyPollerObject.getStack().getId(),
                    execute.getHttpErrorMessage()));
        }
    }

    @Override
    public void handleTimeout(GccResourceReadyPollerObject gccResourceReadyPollerObject) {
        throw new GccResourceCreationException(String.format(
                "Something went wrong. Resource in Gcc '%s' with '%s' operation  not started in a reasonable timeframe on '%s' stack.",
                gccResourceReadyPollerObject.getName(), gccResourceReadyPollerObject.getOperationName(), gccResourceReadyPollerObject.getStack().getId()));
    }

    @Override
    public String successMessage(GccResourceReadyPollerObject gccResourceReadyPollerObject) {
        MDCBuilder.buildMdcContext(gccResourceReadyPollerObject.getStack());
        return String.format("Gcc resource '%s' is ready on '%s' stack",
                gccResourceReadyPollerObject.getName(), gccResourceReadyPollerObject.getStack().getId());
    }
}
