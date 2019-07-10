package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
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
            DescribeFreeIpaResponse freeIpa = freeIpaPollerObject.getFreeIpaV1Endpoint().describe(environmentCrn);
            if (freeIpa == null) {
                throw new FreeIpaOperationFailedException("FreeIpa instance does not exist.");
            } else if (freeIpa.getStatus().isDeletionInProgress() || freeIpa.getStatus().isSuccessfullyDeleted()) {
                LOGGER.info("FreeIpa '{}' '{}' is getting terminated (status:'{}'), polling is cancelled.",
                        freeIpa.getName(),
                        freeIpa.getCrn(),
                        freeIpa.getStatus());
                throw new FreeIpaOperationFailedException("FreeIpa instance deleted under the creation process.");
            } else if (freeIpa.getStatus().isFailed()) {
                LOGGER.info("FreeIpa '{}' '{}' is in failed state (status:'{}'), polling is cancelled.",
                        freeIpa.getName(),
                        freeIpa.getCrn(),
                        freeIpa.getStatus());
                throw new FreeIpaOperationFailedException(String.format("FreeIpa creation failed. Status: '%s' statusReason: '%s'",
                        freeIpa.getStatus(), freeIpa.getStatusReason()));
            } else if (freeIpa.getStatus().isAvailable()) {
                return true;
            }
        } catch (Exception e) {
            throw new FreeIpaOperationFailedException(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public void handleTimeout(FreeIpaPollerObject freeIpaPollerObject) {
        try {
            DescribeFreeIpaResponse freeIpa = freeIpaPollerObject.getFreeIpaV1Endpoint().describe(freeIpaPollerObject.getEnvironmentCrn());
            throw new FreeIpaOperationFailedException(String.format("Polling operation timed out, FreeIpa creation failed. FreeIpa status: '%s' "
                    + "statusReason: '%s'", freeIpa.getStatus(), freeIpa.getStatusReason()));
        } catch (Exception e) {
            throw new FreeIpaOperationFailedException("Polling operation timed out, FreeIpa creation failed. Also failed to get FreeIpa status: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public String successMessage(FreeIpaPollerObject freeIpaPollerObject) {
        return String.format("FreeIpa creation successfully finished '%s'", freeIpaPollerObject.getEnvironmentCrn());
    }

    @Override
    public boolean exitPolling(FreeIpaPollerObject freeIpaPollerObject) {
        PollGroup environmentPollGroup = EnvironmentInMemoryStateStore.get(freeIpaPollerObject.getEnvironmentId());
        if (environmentPollGroup == null || environmentPollGroup.isCancelled()) {
            LOGGER.info("Cancelling the polling of environment's '{}' FreeIpa cluster creation, because a delete operation has already been "
                    + "started on the environment", freeIpaPollerObject.getEnvironmentCrn());
            return true;
        }
        return false;
    }
}
