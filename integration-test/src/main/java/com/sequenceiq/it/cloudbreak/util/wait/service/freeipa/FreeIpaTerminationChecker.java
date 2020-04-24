package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_FAILED;

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
        String crn = waitObject.getFreeIpaCrn();
        Status desiredStatus = waitObject.getDesiredStatus();
        try {
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            LOGGER.info("Waiting for the '{}' state of '{}' freeIpa at '{}' environment. Actual state is: '{}'", desiredStatus, crn, environmentCrn,
                    freeIpa.getStatus());
            if (freeIpa.getStatus() == DELETE_FAILED) {
                throw new TestFailException("FreeIpa termination failed: " + freeIpa.getStatusReason());
            }
            if (!freeIpa.getStatus().isSuccessfullyDeleted()) {
                return false;
            }
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("FreeIpa termination failed: ")
                    .append(System.lineSeparator())
                    .append(e.getMessage())
                    .append(System.lineSeparator())
                    .append(e);
            LOGGER.error(builder.toString());
            throw new TestFailException(builder.toString());
        }
        return true;
    }

    @Override
    public void handleTimeout(T waitObject) {
        try {
            String environmentCrn = waitObject.getEnvironmentCrn();
            String crn = waitObject.getFreeIpaCrn();
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            throw new TestFailException(String.format("Wait operation timed out, '%s' freeIpa termination failed for '%s' environment. FreeIpa status: '%s' " +
                    "statusReason: '%s'", crn, environmentCrn, freeIpa.getStatus(), freeIpa.getStatusReason()));
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Wait operation timed out, freeIpa termination failed. Also failed to get freeIpa status: ")
                    .append(System.lineSeparator())
                    .append(e.getMessage())
                    .append(System.lineSeparator())
                    .append(e);
            LOGGER.error(builder.toString());
            throw new TestFailException(builder.toString());
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("'%s' FreeIpa termination successfully finished.", waitObject.getFreeIpaCrn());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String environmentCrn = waitObject.getEnvironmentCrn();
        try {
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            Status status = freeIpa.getStatus();
            if (status == DELETE_FAILED) {
                return false;
            }
            return status.isFailed();
        } catch (ProcessingException clientException) {
            StringBuilder builder = new StringBuilder("Exit waiting! Failed to get freeIpa due to API client exception: ")
                    .append(System.lineSeparator())
                    .append(clientException.getMessage())
                    .append(System.lineSeparator())
                    .append(clientException);
            LOGGER.error(builder.toString());
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Exit waiting! Failed to get freeIpa, because of: ")
                    .append(System.lineSeparator())
                    .append(e.getMessage())
                    .append(System.lineSeparator())
                    .append(e);
            LOGGER.error(builder.toString());
            return true;
        }
        return false;
    }
}
