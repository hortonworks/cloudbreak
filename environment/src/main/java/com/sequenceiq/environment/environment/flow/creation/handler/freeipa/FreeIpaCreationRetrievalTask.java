package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

public class FreeIpaCreationRetrievalTask extends SimpleStatusCheckerTask<FreeIpaPollerObject> {

    public static final int FREEIPA_RETRYING_INTERVAL = 5000;

    public static final int FREEIPA_RETRYING_COUNT = 240;

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationRetrievalTask.class);

    @Override
    public boolean checkStatus(FreeIpaPollerObject freeIpaPollerObject) {
        String environmentCrn = freeIpaPollerObject.getEnvironmentCrn();
        try {
            LOGGER.info("Checking the state of FreeIpa creation progress for environment: '{}'", environmentCrn);
            if (freeIpaPollerObject.getFreeIpaV1Endpoint().describe(environmentCrn).getStatus().isAvailable()) {
                return true;
            }
        } catch (Exception e) {
            throw new FreeIpaOperationFailedException("FreeIpa creation operation failed", e);
        }
        return false;
    }

    @Override
    public void handleTimeout(FreeIpaPollerObject freeIpaPollerObject) {
        throw new FreeIpaOperationFailedException("Operation timed out. FreeIpa creation was not success in the given time: "
                + freeIpaPollerObject.getEnvironmentCrn());
    }

    @Override
    public String successMessage(FreeIpaPollerObject freeIpaPollerObject) {
        return String.format("FreeIpa creation successfully finished '%s'", freeIpaPollerObject.getEnvironmentCrn());
    }

    @Override
    public boolean exitPolling(FreeIpaPollerObject freeIpaPollerObject) {
        try {
            String environmentCrn = freeIpaPollerObject.getEnvironmentCrn();
            DescribeFreeIpaResponse freeIpaResponse = freeIpaPollerObject
                    .getFreeIpaV1Endpoint()
                    .describe(environmentCrn);
            Status freeIpaResponseStatus = freeIpaResponse.getStatus();

            if (freeIpaResponseStatus.isDeletionInProgress() || freeIpaResponseStatus.isSuccesfullyDeleted()) {
                LOGGER.info("FreeIpa '{}' '{}' is getting terminated (status:'{}'), polling is cancelled.", freeIpaResponse.getName(),
                        freeIpaResponse.getCrn(), freeIpaResponse.getStatus());
                return true;
            } else if (freeIpaResponseStatus.isFailed()) {
                LOGGER.info("FreeIpa '{}' '{}' is in failed state (status:'{}'), polling is cancelled.", freeIpaResponse.getName(),
                        freeIpaResponse.getCrn(), freeIpaResponse.getStatus());
                return true;
            }
        } catch (WebApplicationException | ProcessingException clientException) {
            LOGGER.info("Failed to describe FreeIpa cluster due to API client exception: {}", clientException.getMessage());
        } catch (Exception ex) {
            LOGGER.info("Error occurred during the polling of FreeIpa creation, interrupt polling: ", ex);
            return true;
        }
        return false;
    }
}
