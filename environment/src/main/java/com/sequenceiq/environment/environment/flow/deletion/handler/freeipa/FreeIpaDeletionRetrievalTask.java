package com.sequenceiq.environment.environment.flow.deletion.handler.freeipa;

import java.util.Optional;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.polling.SimpleStatusCheckerTask;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.FreeIpaPollerObject;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

public class FreeIpaDeletionRetrievalTask extends SimpleStatusCheckerTask<FreeIpaPollerObject> {

    public static final int FREEIPA_RETRYING_INTERVAL = 5000;

    public static final int FREEIPA_RETRYING_COUNT = 900;

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDeletionRetrievalTask.class);

    private final FreeIpaService freeIpaService;

    public FreeIpaDeletionRetrievalTask(FreeIpaService freeIpaService) {
        this.freeIpaService = freeIpaService;
    }

    @Override
    public boolean checkStatus(FreeIpaPollerObject freeIpaPollerObject) {
        String environmentCrn = freeIpaPollerObject.getEnvironmentCrn();
        try {
            LOGGER.info("Checking the state of FreeIpa termination progress for environment: '{}'", environmentCrn);
            Optional<DescribeFreeIpaResponse> freeIpaResponse = freeIpaService.describe(environmentCrn);
            if (freeIpaResponse.isPresent()) {
                if (freeIpaResponse.get().getStatus() == Status.DELETE_FAILED) {
                    throw new FreeIpaOperationFailedException("FreeIpa deletion operation failed: " + freeIpaResponse.get().getStatusReason());
                }
                if (!freeIpaResponse.get().getStatus().isSuccessfullyDeleted()) {
                    return false;
                }
            } else {
                LOGGER.info("FreeIpa was not found.");
                return true;
            }
        } catch (Exception e) {
            throw new FreeIpaOperationFailedException("FreeIpa deletion operation failed. " + e.getMessage(), e);
        }
        return true;
    }

    @Override
    public void handleTimeout(FreeIpaPollerObject freeIpaPollerObject) {
        try {
            String envCrn = freeIpaPollerObject.getEnvironmentCrn();
            Optional<DescribeFreeIpaResponse> freeIpa = freeIpaService.describe(envCrn);
            if (freeIpa.isEmpty()) {
                throw new FreeIpaOperationFailedException("FreeIpa was not found for environment: " + envCrn);
            }
            throw new FreeIpaOperationFailedException(String.format("Polling operation timed out, FreeIpa deletion failed. FreeIpa status: '%s' "
                    + "statusReason: '%s'", freeIpa.get().getStatus(), freeIpa.get().getStatusReason()));
        } catch (Exception e) {
            throw new FreeIpaOperationFailedException("Polling operation timed out, FreeIpa deletion failed. Also failed to get FreeIpa status: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public String successMessage(FreeIpaPollerObject freeIpaPollerObject) {
        return "FreeIpa deletion successfully finished.";
    }

    @Override
    public boolean exitPolling(FreeIpaPollerObject freeIpaPollerObject) {
        try {
            String environmentCrn = freeIpaPollerObject.getEnvironmentCrn();
            Optional<DescribeFreeIpaResponse> freeIpaResponse = freeIpaService.describe(environmentCrn);
            if (freeIpaResponse.isEmpty()) {
                LOGGER.info("FreeIpa was not found for environment '{}'. Exiting polling", environmentCrn);
                return false;
            }
            Status status = freeIpaResponse.get().getStatus();
            if (status == Status.DELETE_FAILED || status == Status.CREATE_FAILED) {
                return false;
            }
            return status.isFailed();
        } catch (ProcessingException clientException) {
            LOGGER.error("Failed to describe FreeIpa cluster due to API client exception: {}.", clientException.getMessage());
        } catch (Exception e) {
            LOGGER.error("Exception occurred during describing FreeIpa for environment '{}'.", freeIpaPollerObject.getEnvironmentCrn(), e);
            return true;
        }
        return false;
    }
}
