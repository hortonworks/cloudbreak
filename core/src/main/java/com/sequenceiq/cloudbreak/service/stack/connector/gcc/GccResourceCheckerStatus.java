package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class GccResourceCheckerStatus extends StackBasedStatusCheckerTask<GccResourceReadyPollerObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GccResourceCheckerStatus.class);
    private static final int FINISHED = 100;

    @Override
    public boolean checkStatus(GccResourceReadyPollerObject gccResourceReadyPollerObject) {
        LOGGER.info("Checking the status of Gcp resource '{}' [{}].", gccResourceReadyPollerObject.getName(),
                gccResourceReadyPollerObject.getResourceType().name());
        Operation operation = null;
        try {
            if (gccResourceReadyPollerObject.getZoneOperations().isPresent()) {
                operation = gccResourceReadyPollerObject.getZoneOperations().get().execute();
            } else if (gccResourceReadyPollerObject.getRegionOperations().isPresent()) {
                operation = gccResourceReadyPollerObject.getRegionOperations().get().execute();
            }
            return analyzeOperation(operation);
        } catch (Exception e) {
            throw new GcpResourceException("Error during status check",
                    gccResourceReadyPollerObject.getResourceType(),
                    gccResourceReadyPollerObject.getName(),
                    gccResourceReadyPollerObject.getStack().getId(),
                    gccResourceReadyPollerObject.getOperationName(),
                    e);
        }
    }

    @Override
    public void handleTimeout(GccResourceReadyPollerObject gccResourceReadyPollerObject) {
        throw new GcpResourceException("Timeout during status check!",
                gccResourceReadyPollerObject.getResourceType(),
                gccResourceReadyPollerObject.getName(),
                gccResourceReadyPollerObject.getStack().getId(),
                gccResourceReadyPollerObject.getOperationName());
    }

    @Override
    public String successMessage(GccResourceReadyPollerObject gccResourceReadyPollerObject) {
        return String.format("Gcc resource '%s' [%s] is ready on '%s' stack",
                gccResourceReadyPollerObject.getName(), gccResourceReadyPollerObject.getResourceType().name(), gccResourceReadyPollerObject.getStack().getId());
    }

    private boolean analyzeOperation(Operation operation) throws Exception {
        String errorMessage = checkForErrors(operation);
        if (errorMessage != null) {
            throw new Exception(errorMessage);
        } else {
            Integer progress = operation.getProgress();
            return (progress.intValue() != FINISHED) ? false : true;
        }
    }

    private String checkForErrors(Operation operation) {
        String msg = null;
        if (operation == null) {
            LOGGER.error("Operation is null!");
            return msg;
        }
        if (operation.getError() != null) {
            StringBuilder error = new StringBuilder();
            if (operation.getError().getErrors() != null) {
                for (Operation.Error.Errors errors : operation.getError().getErrors()) {
                    error.append(String.format("code: %s -> message: %s %s", errors.getCode(), errors.getMessage(), System.lineSeparator()));
                }
                msg = error.toString();
            } else {
                LOGGER.debug("No errors found, Error: {}", operation.getError());
            }
        }
        if (operation.getHttpErrorStatusCode() != null) {
            msg += String.format(" HTTP error message: %s, HTTP error status code: %s", operation.getHttpErrorMessage(), operation.getHttpErrorStatusCode());
        }
        return msg;
    }
}
