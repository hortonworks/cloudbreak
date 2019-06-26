package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

public class FreeIpaCreationRetrievalTask extends SimpleStatusCheckerTask<FreeIpaPollerObject> {

    public static final int FREEIPA_RETRYING_INTERVAL = 5000;

    public static final int FREEIPA_RETRYING_COUNT = 240;

    public static final int FREEIPA_FAILURE_COUNT = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationRetrievalTask.class);

    @Override
    public boolean checkStatus(FreeIpaPollerObject freeIpaPollerObject) {
        String environmentCrn = freeIpaPollerObject.getEnvironmentCrn();
        try {
            LOGGER.info("Checking the state of FreeIpa creation progress for environment: '{}'", environmentCrn);
            DescribeFreeIpaResponse describe = freeIpaPollerObject.getFreeIpaV1Endpoint().describe(environmentCrn);
            if (describe == null) {
                throw new FreeIpaOperationFailedException("FreeIpa instance does not exist.");
            } else if (describe.getStatus().isDeletionInProgress() || describe.getStatus().isSuccessfullyDeleted()) {
                LOGGER.info("FreeIpa '{}' '{}' is getting terminated (status:'{}'), polling is cancelled.",
                        describe.getName(),
                        describe.getCrn(),
                        describe.getStatus());
                throw new FreeIpaOperationFailedException("FreeIpa instance deleted under the creation process.");
            } else if (describe.getStatus().isFailed()) {
                LOGGER.info("FreeIpa '{}' '{}' is in failed state (status:'{}'), polling is cancelled.",
                        describe.getName(),
                        describe.getCrn(),
                        describe.getStatus());
                throw new FreeIpaOperationFailedException(describe.getStatusReason());
            } else if (describe.getStatus().isAvailable()) {
                return true;
            }
        } catch (Exception e) {
            throw new FreeIpaOperationFailedException(e.getMessage(), e);
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
        return false;
    }
}
