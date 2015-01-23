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
        LOGGER.info("Checking status of Gcc resource '{}' [{}].", gccResourceReadyPollerObject.getName(),
                gccResourceReadyPollerObject.getResourceType().name());
        Operation operation = null;
        try {
            operation = gccResourceReadyPollerObject.getZoneOperations().execute();
            return analyzeOperation(operation, gccResourceReadyPollerObject);
        } catch (IOException e) {
            throw new GccResourceCreationException(String.format(
                    "Something went wrong. Resource in Gcc '%s' [%s] with '%s' operation failed on '%s' stack.",
                    gccResourceReadyPollerObject.getName(),
                    gccResourceReadyPollerObject.getResourceType().name(),
                    gccResourceReadyPollerObject.getOperationName(),
                    gccResourceReadyPollerObject.getStack().getId()));
        }
    }

    @Override
    public void handleTimeout(GccResourceReadyPollerObject gccResourceReadyPollerObject) {
        throw new GccResourceCreationException(String.format(
                "Something went wrong. Resource in Gcc '%s' [%s] with '%s' operation  not started in a reasonable timeframe on '%s' stack.",
                gccResourceReadyPollerObject.getName(),
                gccResourceReadyPollerObject.getResourceType().name(),
                gccResourceReadyPollerObject.getOperationName(),
                gccResourceReadyPollerObject.getStack().getId()));
    }

    @Override
    public String successMessage(GccResourceReadyPollerObject gccResourceReadyPollerObject) {
        MDCBuilder.buildMdcContext(gccResourceReadyPollerObject.getStack());
        return String.format("Gcc resource '%s' [%s] is ready on '%s' stack",
                gccResourceReadyPollerObject.getName(), gccResourceReadyPollerObject.getResourceType().name(), gccResourceReadyPollerObject.getStack().getId());
    }


    private boolean analyzeOperation(Operation operation, GccResourceReadyPollerObject gccResourceReadyPollerObject) {
        MDCBuilder.buildMdcContext(gccResourceReadyPollerObject.getStack());
        if (operation.getHttpErrorStatusCode() != null || operation.getError() != null) {
            StringBuilder error = new StringBuilder();
            if (operation.getError() != null) {
                if (operation.getError().getErrors() != null) {
                    for (Operation.Error.Errors errors : operation.getError().getErrors()) {
                        error.append(String.format("code: %s -> message: %s %s", errors.getCode(), errors.getMessage(), System.lineSeparator()));
                    }
                }
            }
            throw new GccResourceCreationException(String.format(
                    "Something went wrong. Resource in Gcc '%s' [%s] with '%s' operation failed on '%s' stack with %s message: %s",
                    gccResourceReadyPollerObject.getName(),
                    gccResourceReadyPollerObject.getResourceType().name(),
                    gccResourceReadyPollerObject.getOperationName(),
                    gccResourceReadyPollerObject.getStack().getId(),
                    operation.getHttpErrorMessage(),
                    error.toString()));
        } else {
            Integer progress = operation.getProgress();
            return (progress.intValue() != FINISHED) ? false : true;
        }

    }
}
