package com.sequenceiq.environment.environment.flow.deletion.handler.freeipa;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaPollerObject;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

public class FreeIpaDeleteRetrievalTask extends SimpleStatusCheckerTask<FreeIpaPollerObject> {

    public static final int FREEIPA_RETRYING_INTERVAL = 5000;

    public static final int FREEIPA_RETRYING_COUNT = 900;

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDeleteRetrievalTask.class);

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public FreeIpaDeleteRetrievalTask(WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    @Override
    public boolean checkStatus(FreeIpaPollerObject freeIpaPollerObject) {
        String environmentCrn = freeIpaPollerObject.getEnvironmentCrn();
        try {
            LOGGER.info("Checking the state of FreeIpa termination progress for environment: '{}'", environmentCrn);
            DescribeFreeIpaResponse freeIpaResponse = freeIpaPollerObject.getFreeIpaV1Endpoint().describe(environmentCrn);
            if (freeIpaResponse != null) {
                if (freeIpaResponse.getStatus().equals(Status.DELETE_FAILED)) {
                    throw new FreeIpaOperationFailedException("FreeIpa deletion operation failed: " + freeIpaResponse.getStatusReason());
                }
                if (!freeIpaResponse.getStatus().isSuccessfullyDeleted()) {
                    return false;
                }
            } else {
                LOGGER.info("Freeipa response is null");
            }
        } catch (NotFoundException nfe) {
            return true;
        } catch (FreeIpaOperationFailedException fiofe) {
            throw fiofe;
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            throw new FreeIpaOperationFailedException("FreeIpa deletion operation failed. " + e.getMessage() + ". " + errorMessage, e);
        } catch (Exception e) {
            throw new FreeIpaOperationFailedException("FreeIpa deletion operation failed. " + e.getMessage(), e);
        }
        return true;
    }

    @Override
    public void handleTimeout(FreeIpaPollerObject freeIpaPollerObject) {
        try {
            DescribeFreeIpaResponse freeIpa = freeIpaPollerObject.getFreeIpaV1Endpoint().describe(freeIpaPollerObject.getEnvironmentCrn());
            throw new FreeIpaOperationFailedException(String.format("Polling operation timed out, FreeIpa deletion failed. FreeIpa status: '%s' "
                    + "statusReason: '%s'", freeIpa.getStatus(), freeIpa.getStatusReason()));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            throw new FreeIpaOperationFailedException("Polling operation timed out, FreeIpa deletion failed. Also failed to get FreeIpa status: "
                    + e.getMessage() + ". " + errorMessage, e);
        } catch (Exception e) {
            throw new FreeIpaOperationFailedException("Polling operation timed out, FreeIpa deletion failed. Also failed to get FreeIpa status: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public String successMessage(FreeIpaPollerObject freeIpaPollerObject) {
        return "FreeIpa deletion successfully finished";
    }

    @Override
    public boolean exitPolling(FreeIpaPollerObject freeIpaPollerObject) {
        try {
            String environmentCrn = freeIpaPollerObject.getEnvironmentCrn();
            Status status = freeIpaPollerObject.getFreeIpaV1Endpoint().describe(environmentCrn).getStatus();
            if (status.equals(Status.DELETE_FAILED) || status.equals(Status.CREATE_FAILED)) {
                return false;
            }
            return status.isFailed();
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.info("Failed to describe FreeIpa cluster due to API client exception: {}. {}.", e.getMessage(), errorMessage);
        } catch (ProcessingException clientException) {
            LOGGER.info("Failed to describe FreeIpa cluster due to API client exception: {}.", clientException.getMessage());
        } catch (Exception e) {
            return true;
        }
        return false;
    }
}
