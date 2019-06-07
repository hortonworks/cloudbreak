package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;

public class FreeIpaCreationRetrievalTask extends SimpleStatusCheckerTask<FreeIpaPollerObject> {

    public static final int FREEIPA_RETRYING_INTERVAL = 5000;

    public static final int FREEIPA_RETRYING_COUNT = 240;

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationRetrievalTask.class);

    @Override
    public boolean checkStatus(FreeIpaPollerObject freeIpaPollerObject) {
        String environmentCrn = freeIpaPollerObject.getEnvironmentCrn();
        try {
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
