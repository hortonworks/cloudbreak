package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class GcpResourceCheckerStatus extends StackBasedStatusCheckerTask<GcpResourceReadyPollerObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpResourceCheckerStatus.class);
    private static final int FINISHED = 100;

    @Override
    public boolean checkStatus(GcpResourceReadyPollerObject gcpResourceReadyPollerObject) {
        LOGGER.info("Checking the status of Gcp resource '{}' [{}].", gcpResourceReadyPollerObject.getName(),
                gcpResourceReadyPollerObject.getResourceType().name());
        Operation operation = null;
        try {
            if (gcpResourceReadyPollerObject.getZoneOperations().isPresent()) {
                operation = gcpResourceReadyPollerObject.getZoneOperations().get().execute();
            } else if (gcpResourceReadyPollerObject.getRegionOperations().isPresent()) {
                operation = gcpResourceReadyPollerObject.getRegionOperations().get().execute();
            } else if (gcpResourceReadyPollerObject.getGlobalOperations().isPresent()) {
                operation = gcpResourceReadyPollerObject.getGlobalOperations().get().execute();
            }
            return analyzeOperation(operation);
        } catch (Exception e) {
            throw new GcpResourceException("Error during status check",
                    gcpResourceReadyPollerObject.getResourceType(),
                    gcpResourceReadyPollerObject.getName(),
                    gcpResourceReadyPollerObject.getStack().getId(),
                    gcpResourceReadyPollerObject.getOperationName(),
                    e);
        }
    }

    @Override
    public void handleTimeout(GcpResourceReadyPollerObject gcpResourceReadyPollerObject) {
        throw new GcpResourceException("Timeout during status check!",
                gcpResourceReadyPollerObject.getResourceType(),
                gcpResourceReadyPollerObject.getName(),
                gcpResourceReadyPollerObject.getStack().getId(),
                gcpResourceReadyPollerObject.getOperationName());
    }

    @Override
    public String successMessage(GcpResourceReadyPollerObject gcpResourceReadyPollerObject) {
        return String.format("Gcp resource '%s' [%s] is ready on '%s' stack",
                gcpResourceReadyPollerObject.getName(), gcpResourceReadyPollerObject.getResourceType().name(), gcpResourceReadyPollerObject.getStack().getId());
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
