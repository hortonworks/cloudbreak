package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.model.Operation;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.service.SimpleStatusCheckerTask;

@Component
public class GcpRemoveCheckerStatus extends SimpleStatusCheckerTask<GcpRemoveReadyPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpRemoveCheckerStatus.class);
    private static final int FINISHED = 100;
    private static final int NOT_FOUND = 404;

    @Override
    public boolean checkStatus(GcpRemoveReadyPollerObject gcpRemoveReadyPollerObject) {
        LOGGER.info("Checking resource removal status. [ resource: {}, resourceType: {}, stackId: {}", gcpRemoveReadyPollerObject.getName(),
                gcpRemoveReadyPollerObject.getResourceType().name(), gcpRemoveReadyPollerObject.getStack().getId());
        try {
            Optional<Operation> operation = executeOperations(gcpRemoveReadyPollerObject);
            return analyzeOperation(operation);
        } catch (Exception e) {
            throw new GcpResourceException("GCP resource operation failed!", gcpRemoveReadyPollerObject.getResourceType(), gcpRemoveReadyPollerObject.getName(),
                    gcpRemoveReadyPollerObject.getStack().getId(), gcpRemoveReadyPollerObject.getOperationName(), e);
        }
    }

    private Optional<Operation> executeOperations(GcpRemoveReadyPollerObject gcpRemoveReadyPollerObject) throws Exception {
        Optional<Operation> operation = Optional.<Operation>absent();
        try {
            if (gcpRemoveReadyPollerObject.getZoneOperations().isPresent()) {
                LOGGER.debug("Executing zone operations: {}", gcpRemoveReadyPollerObject.getZoneOperations().asSet());
                operation = Optional.fromNullable(gcpRemoveReadyPollerObject.getZoneOperations().get().execute());
            } else if (gcpRemoveReadyPollerObject.getRegionOperations().isPresent()) {
                LOGGER.debug("Executing region operations: {}", gcpRemoveReadyPollerObject.getRegionOperations().asSet());
                operation = Optional.fromNullable(gcpRemoveReadyPollerObject.getRegionOperations().get().execute());
            } else {
                LOGGER.error("No operations found for execution!");
            }
        } catch (SocketTimeoutException exception) {
            operation = Optional.absent();
        } catch (GoogleJsonResponseException e) {
            LOGGER.debug("Checking the GCP exception: {}", e.getStatusMessage());
            if (e.getDetails().get("code").equals(NOT_FOUND)) {
                LOGGER.debug("Executing global operations: {}", gcpRemoveReadyPollerObject.getGlobalOperations().keySet());
                operation = Optional.fromNullable(gcpRemoveReadyPollerObject.getGlobalOperations().execute());
            } else {
                LOGGER.debug("Bypassing the GCP exception.");
                throw e;
            }
        }
        return operation;
    }

    @Override
    public void handleTimeout(GcpRemoveReadyPollerObject gcpRemoveReadyPollerObject) {
        throw new GcpResourceException("Failed to remove resource; operation timed out!", gcpRemoveReadyPollerObject.getResourceType(),
                gcpRemoveReadyPollerObject.getName(), gcpRemoveReadyPollerObject.getStack().getId(), gcpRemoveReadyPollerObject.getOperationName());
    }

    @Override
    public String successMessage(GcpRemoveReadyPollerObject gcpRemoveReadyPollerObject) {
        return String.format("Gcp resource '%s'[%s] is successfully removed from stack '%s'", gcpRemoveReadyPollerObject.getName(),
                gcpRemoveReadyPollerObject.getResourceType(), gcpRemoveReadyPollerObject.getStack().getId());
    }

    @Override
    public boolean exitPolling(GcpRemoveReadyPollerObject gcpRemoveReadyPollerObject) {
        return false;
    }

    private boolean analyzeOperation(Optional<Operation> operation) throws Exception {
        String errorMessage = checkForErrors(operation);
        if (errorMessage != null) {
            throw new Exception(errorMessage);
        } else if (!operation.isPresent()) {
            return false;
        } else {
            Integer progress = operation.orNull().getProgress();
            return (progress.intValue() != FINISHED) ? false : true;
        }
    }

    private String checkForErrors(Optional<Operation> operation) {
        String msg = null;
        if (!operation.isPresent()) {
            LOGGER.error("Operation is null!");
            return msg;
        }
        if (operation.get().getError() != null) {
            StringBuilder error = new StringBuilder();
            if (operation.get().getError().getErrors() != null) {
                for (Operation.Error.Errors errors : operation.get().getError().getErrors()) {
                    error.append(String.format("code: %s -> message: %s %s", errors.getCode(), errors.getMessage(), System.lineSeparator()));
                }
                msg = error.toString();
            } else {
                LOGGER.debug("No errors found, Error: {}", operation.get().getError());
            }
        }
        if (operation.get().getHttpErrorStatusCode() != null) {
            msg += String.format(" HTTP error message: %s, HTTP error status code: %s",
                    operation.get().getHttpErrorMessage(), operation.get().getHttpErrorStatusCode());
        }
        return msg;
    }
}
