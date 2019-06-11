package com.sequenceiq.environment.environment.flow.delete.handler.freeipa;

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
            if (freeIpaPollerObject.getFreeIpaV1Endpoint().describe(environmentCrn).getStatus().isAvailable()) {
                return false;
            }
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
            if (freeIpaPollerObject.getFreeIpaV1Endpoint().describe(environmentCrn).getStatus().isFailed()) {
                LOGGER.debug("Stack is getting terminated, polling is cancelled.");
                return true;
            }
            return false;
        } catch (Exception ex) {
            LOGGER.info("Error occurred when check status checker exit criteria: ", ex);
            return true;
        }
    }
}
