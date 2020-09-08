package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.CREATE_FAILED;

import java.util.Map;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class FreeIpaOperationChecker<T extends FreeIpaWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaOperationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String environmentCrn = waitObject.getEnvironmentCrn();
        Status desiredStatus = waitObject.getDesiredStatus();
        try {
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            if (freeIpa == null) {
                LOGGER.error("No freeIpa found with environmentCrn '{}'! Check '{}' status.", environmentCrn, desiredStatus);
                throw new TestFailException(String.format("No freeIpa found with environmentCrn '%s'! Check '%s' status.", environmentCrn, desiredStatus));
            }
            String crn = freeIpa.getCrn();
            String name = freeIpa.getName();
            Status status = freeIpa.getStatus();
            LOGGER.info("Waiting for the '{}' state of '{}' '{}' freeIpa at '{}' environment. Actual state is: '{}'", desiredStatus, name, crn, environmentCrn,
                    freeIpa.getStatus());
            if (freeIpa.getStatus().isDeletionInProgress() || freeIpa.getStatus().isSuccessfullyDeleted()) {
                LOGGER.error("FreeIpa '{}' '{}' has been getting terminated (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("FreeIpa '%s' '%s' has been getting terminated (status:'%s'), waiting is cancelled.", name, crn,
                        status));
            }
            if (freeIpa.getStatus().isFailed()) {
                LOGGER.error("FreeIpa '{}' '{}' is in failed state (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("FreeIpa '%s' '%s' is in failed state. Status: '%s' statusReason: '%s'", name, crn, status,
                        freeIpa.getStatusReason()));
            }
            if (desiredStatus.equals(status)) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("FreeIpa creation failed. Also failed to get freeIpa status: {}", e.getMessage(), e);
            throw new TestFailException("FreeIpa creation failed. Also failed to get freeIpa status", e);
        }
        return false;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String environmentCrn = waitObject.getEnvironmentCrn();
        try {
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            if (freeIpa == null) {
                LOGGER.error("No freeIpa found with environmentCrn '{}'! Wait operation timed out.", environmentCrn);
                throw new TestFailException(String.format("No freeIpa found with environmentCrn '%s'! Wait operation timed out.", environmentCrn));
            }
            throw new TestFailException(String.format("Wait operation timed out, freeIpa '%s' '%s' has been failed for environment '%s'. FreeIpa status: '%s' "
                    + "statusReason: '%s'", freeIpa.getName(), freeIpa.getCrn(), environmentCrn, freeIpa.getStatus(), freeIpa.getStatusReason()));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out, freeIpa has been failed. Also failed to get freeIpa status: {}", e.getMessage(), e);
            throw new TestFailException("Wait operation timed out, freeIpa has been failed. Also failed to get freeIpa status", e);
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation has successfully been done with '%s' freeIpa state for '%s' environment.", waitObject.getDesiredStatus(),
                waitObject.getEnvironmentCrn());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String environmentCrn = waitObject.getEnvironmentCrn();
        try {
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            if (freeIpa == null) {
                LOGGER.info("No freeIpa found with environmentCrn '{}'! Exit waiting.", environmentCrn);
                return true;
            }
            Status status = freeIpa.getStatus();
            if (status.equals(CREATE_FAILED)) {
                return true;
            }
            return status.isFailed();
        } catch (ProcessingException clientException) {
            LOGGER.error("Exit waiting! Failed to describe freeIpa cluster due to API client exception: {}", clientException.getMessage(), clientException);
        } catch (Exception e) {
            LOGGER.error("Exit waiting! Exception occurred during describing freeIpa for environment: {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return Map.of("status", waitObject.getEndpoint().describe(waitObject.getEnvironmentCrn()).getStatus().name());
    }
}
