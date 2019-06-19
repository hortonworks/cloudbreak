package com.sequenceiq.environment.environment.flow.delete.handler.freeipa;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaPollerObject;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;

public class FreeIpaDeleteRetrievalTask extends SimpleStatusCheckerTask<FreeIpaPollerObject> {

    public static final int FREEIPA_RETRYING_INTERVAL = 5000;

    public static final int FREEIPA_RETRYING_COUNT = 240;

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDeleteRetrievalTask.class);

    @Override
    public boolean checkStatus(FreeIpaPollerObject freeIpaPollerObject) {
        String environmentCrn = freeIpaPollerObject.getEnvironmentCrn();
        try {
            LOGGER.info("Checking the state of FreeIpa termination progress for environment: '{}'", environmentCrn);
            if (getIfNotNull(freeIpaPollerObject.getFreeIpaV1Endpoint().describe(environmentCrn), response -> !response.getStatus().isSuccesfullyDeleted())) {
                return false;
            }
        } catch (NotFoundException nfe) {
            return true;
        } catch (Exception e) {
            throw new FreeIpaOperationFailedException("FreeIpa delete operation failed", e);
        }
        return true;
    }

    @Override
    public void handleTimeout(FreeIpaPollerObject freeIpaPollerObject) {
        throw new FreeIpaOperationFailedException("Operation timed out. FreeIpa delete did not succeeded in the given time: "
                + freeIpaPollerObject.getEnvironmentCrn());
    }

    @Override
    public String successMessage(FreeIpaPollerObject freeIpaPollerObject) {
        return "FreeIpa delete successfully finished";
    }

    @Override
    public boolean exitPolling(FreeIpaPollerObject freeIpaPollerObject) {
        try {
            String environmentCrn = freeIpaPollerObject.getEnvironmentCrn();
            return freeIpaPollerObject.getFreeIpaV1Endpoint().describe(environmentCrn).getStatus().isFailed();
        } catch (WebApplicationException | ProcessingException clientException) {
            LOGGER.info("Failed to describe FreeIpa cluster due to API client exception: {}", clientException.getMessage());
        } catch (Exception e) {
            return true;
        }
        return false;
    }
}
