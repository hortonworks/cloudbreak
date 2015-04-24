package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import java.io.IOException;

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
        LOGGER.info("Checking status of remove '{}' {} on '{}' stack.",
                gccRemoveReadyPollerObject.getName(),
                gccRemoveReadyPollerObject.getResourceType().name(),
                gccRemoveReadyPollerObject.getStack().getId());
        try {
            Operation operation = null;
            if (gccRemoveReadyPollerObject.getZoneOperations().isPresent()) {
                operation = gccRemoveReadyPollerObject.getZoneOperations().orNull().execute();
            } else if (gccRemoveReadyPollerObject.getRegionOperations().isPresent()) {
                operation = gccRemoveReadyPollerObject.getRegionOperations().orNull().execute();
            }
            return analyzeOperation(operation, gccRemoveReadyPollerObject);
        } catch (GoogleJsonResponseException ex) {
            return exceptionHandler(ex, gccRemoveReadyPollerObject);
        } catch (NullPointerException | IOException e) {
            throw new GccResourceRemoveException(String.format(
                    "Something went wrong. Resource in Gcc '%s'[%s] with '%s' operation failed on '%s' stack.",
                    gccRemoveReadyPollerObject.getName(),
                    gccRemoveReadyPollerObject.getResourceType().name(),
                    gccRemoveReadyPollerObject.getOperationName(),
                    gccRemoveReadyPollerObject.getStack().getId()));
        }
    }

    private boolean exceptionHandler(GoogleJsonResponseException ex, GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        if (ex.getDetails().get("code").equals(NOT_FOUND)) {
            try {
                Operation operation = gccRemoveReadyPollerObject.getGlobalOperations().execute();
                return analyzeOperation(operation, gccRemoveReadyPollerObject, ex);
            } catch (IOException e) {
                throw new GccResourceRemoveException(String.format(
                        "Something went wrong. Resource in Gcc '%s'[%s] with '%s' operation failed on '%s' stack.",
                        gccRemoveReadyPollerObject.getName(),
                        gccRemoveReadyPollerObject.getResourceType().name(),
                        gccRemoveReadyPollerObject.getOperationName(),
                        gccRemoveReadyPollerObject.getStack().getId()));
            }
        } else {
            throw new GccResourceRemoveException(String.format(
                    "Something went wrong. Resource in Gcc '%s'[%s] with '%s' operation failed on '%s' stack.",
                    gccRemoveReadyPollerObject.getName(),
                    gccRemoveReadyPollerObject.getResourceType().name(),
                    gccRemoveReadyPollerObject.getOperationName(),
                    gccRemoveReadyPollerObject.getStack().getId()));
        }
    }

    @Override
    public void handleTimeout(GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        throw new GccResourceRemoveException(String.format(
                "Something went wrong. Remove of '%s'[%s] resource unsuccess in a reasonable timeframe on '%s' stack.",
                gccRemoveReadyPollerObject.getName(), gccRemoveReadyPollerObject.getResourceType(), gccRemoveReadyPollerObject.getStack().getId()));
    }

    @Override
    public String successMessage(GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        return String.format("Gcc resource '%s'[%s] is removed success on '%s' stack",
                gccRemoveReadyPollerObject.getName(), gccRemoveReadyPollerObject.getResourceType(), gccRemoveReadyPollerObject.getStack().getId());
    }

    @Override
    public boolean exitPolling(GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        return false;
    }

    private boolean analyzeOperation(Operation operation, GccRemoveReadyPollerObject gccRemoveReadyPollerObject, GoogleJsonResponseException ex) {
        if (ex != null && ex.getDetails().get("code").equals(NOT_FOUND)) {
            return true;
        } else {
            return analyzeOperation(operation, gccRemoveReadyPollerObject);
        }
    }

    private boolean analyzeOperation(Operation operation, GccRemoveReadyPollerObject gccRemoveReadyPollerObject) {
        if (operation.getHttpErrorStatusCode() != null || operation.getError() != null) {
            StringBuilder error = new StringBuilder();
            if (operation.getError() != null) {
                if (operation.getError().getErrors() != null) {
                    for (Operation.Error.Errors errors : operation.getError().getErrors()) {
                        error.append(String.format("code: %s -> message: %s %s", errors.getCode(), errors.getMessage(), System.lineSeparator()));
                    }
                }
            }
            throw new GccResourceRemoveException(String.format(
                    "Something went wrong. Resource in Gcc '%s' [%s] with '%s' operation failed on '%s' stack with %s message: %s",
                    gccRemoveReadyPollerObject.getName(),
                    gccRemoveReadyPollerObject.getResourceType().name(),
                    gccRemoveReadyPollerObject.getOperationName(),
                    gccRemoveReadyPollerObject.getStack().getId(),
                    operation.getHttpErrorMessage(),
                    error.toString()));
        } else {
            Integer progress = operation.getProgress();
            return (progress.intValue() != FINISHED) ? false : true;
        }

    }
}
