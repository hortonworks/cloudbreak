package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

@Component
public class GccRemoveCheckerStatus implements StatusCheckerTask<GccRemoveReadyPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccRemoveCheckerStatus.class);
    private static final int FINISHED = 100;
    private static final int NOT_FOUND = 404;

    @Override
    public boolean checkStatus(GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        LOGGER.info("Checking resource removal status. [ resource: {}, resourceType: {}, stackId: {}", gccRemoveReadyPollerObject.getName(),
                gccRemoveReadyPollerObject.getResourceType().name(), gccRemoveReadyPollerObject.getStack().getId());
        try {
            Operation operation = executeOperations(gccRemoveReadyPollerObject);
            return analyzeOperation(operation);
        } catch (Exception e) {
            throw new GcpResourceException("GCP resource operation failed!", gccRemoveReadyPollerObject.getResourceType(), gccRemoveReadyPollerObject.getName(),
                    gccRemoveReadyPollerObject.getStack().getId(), gccRemoveReadyPollerObject.getOperationName(), e);
        }
    }

    private Operation executeOperations(GccRemoveReadyPollerObject gcpRemoveReadyPollerObject) throws Exception {
        Operation operation = null;
        try {
            if (gcpRemoveReadyPollerObject.getZoneOperations().isPresent()) {
                LOGGER.debug("Executing zone operations: {}", gcpRemoveReadyPollerObject.getZoneOperations().asSet());
                operation = gcpRemoveReadyPollerObject.getZoneOperations().get().execute();
            } else if (gcpRemoveReadyPollerObject.getRegionOperations().isPresent()) {
                LOGGER.debug("Executing region operations: {}", gcpRemoveReadyPollerObject.getRegionOperations().asSet());
                operation = gcpRemoveReadyPollerObject.getRegionOperations().get().execute();
            } else {
                LOGGER.error("No operations found for execution!");
            }
        } catch (GoogleJsonResponseException e) {
            LOGGER.debug("Checking the GCP exception", e);
            if (e.getDetails().get("code").equals(NOT_FOUND)) {
                LOGGER.debug("Executing global operations: {}", gcpRemoveReadyPollerObject.getGlobalOperations().keySet());
                operation = gcpRemoveReadyPollerObject.getGlobalOperations().execute();
            } else {
                LOGGER.debug("Bypassing the GCP exception.");
                throw e;
            }
        }
        return operation;
    }

    @Override
    public void handleTimeout(GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        throw new GcpResourceException("Failed to remove resource; operation timed out!", gccRemoveReadyPollerObject.getResourceType(),
                gccRemoveReadyPollerObject.getName(), gccRemoveReadyPollerObject.getStack().getId(), gccRemoveReadyPollerObject.getOperationName());
    }

    @Override
    public String successMessage(GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        return String.format("Gcc resource '%s'[%s] is successfully removed from stack '%s'", gccRemoveReadyPollerObject.getName(),
                gccRemoveReadyPollerObject.getResourceType(), gccRemoveReadyPollerObject.getStack().getId());
    }

    @Override
    public boolean exitPolling(GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        return false;
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
