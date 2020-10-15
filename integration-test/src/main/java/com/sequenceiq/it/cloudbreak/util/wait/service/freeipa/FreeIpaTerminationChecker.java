package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_FAILED;

import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class FreeIpaTerminationChecker<T extends FreeIpaWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaTerminationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String environmentCrn = waitObject.getEnvironmentCrn();
        Status desiredStatus = waitObject.getDesiredStatus();
        try {
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            LOGGER.info("Waiting for the '{}' state of '{}' freeIpa at '{}' environment. Actual state is: '{}'", desiredStatus, freeIpa.getCrn(),
                    environmentCrn, freeIpa.getStatus());
            if (freeIpa.getStatus().equals(DELETE_FAILED)) {
                throw new TestFailException("FreeIpa termination failed: " + freeIpa.getStatusReason());
            }
            if (!freeIpa.getStatus().isSuccessfullyDeleted()) {
                return false;
            }
        } catch (NotFoundException e) {
            LOGGER.warn("No freeIpa found with environmentCrn '{}'! It has been deleted successfully.", environmentCrn, e);
        } catch (Exception e) {
            LOGGER.error("FreeIpa termination failed: {}", e.getMessage(), e);
            throw new TestFailException("FreeIpa termination failed", e);
        }
        return true;
    }

    @Override
    public void handleTimeout(T waitObject) {
        try {
            String environmentCrn = waitObject.getEnvironmentCrn();
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            throw new TestFailException(String.format("Wait operation timed out, '%s' freeIpa termination failed for '%s' environment. FreeIpa status: '%s' " +
                    "statusReason: '%s'", freeIpa.getCrn(), environmentCrn, freeIpa.getStatus(), freeIpa.getStatusReason()));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out, freeIpa termination failed: {}", e.getMessage(), e);
            throw new TestFailException("Wait operation timed out, freeIpa termination failed", e);
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("FreeIpa termination have successfully been finished for '%s' environment.", waitObject.getEnvironmentCrn());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String environmentCrn = waitObject.getEnvironmentCrn();
        try {
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            Status status = freeIpa.getStatus();
            if (status.equals(DELETE_FAILED)) {
                return true;
            }
            return status.isFailed();
        } catch (ProcessingException clientException) {
            LOGGER.error("Exit waiting! Failed to get freeIpa due to API client exception: {}", clientException.getMessage(), clientException);
        } catch (Exception e) {
            LOGGER.warn("Exit waiting! Failed to get freeIpa, because of: {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        String environmentCrn = waitObject.getEnvironmentCrn();
        try {
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            return Map.of("status", freeIpa.getStatus().name());
        } catch (NotFoundException e) {
            LOGGER.warn("No freeIpa found with environmentCrn '{}'! It has been deleted successfully.", environmentCrn, e);
            return Map.of("status", DELETE_COMPLETED.name());
        }
    }
}
