package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa;

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
        String crn = waitObject.getFreeIpaCrn();
        Status desiredStatus = waitObject.getDesiredStatus();
        try {
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            if (freeIpa == null) {
                throw new TestFailException(String.format("'%s' freeIpa cluster was not found for environment '%s'", crn, environmentCrn));
            }
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
            StringBuilder builder = new StringBuilder("FreeIpa creation failed. Also failed to get freeIpa status:  ")
                    .append(System.lineSeparator())
                    .append(e.getMessage())
                    .append(System.lineSeparator())
                    .append(e);
            LOGGER.error(builder.toString());
            throw new TestFailException(builder.toString());
        }
        return false;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String environmentCrn = waitObject.getEnvironmentCrn();
        String crn = waitObject.getFreeIpaCrn();
        try {
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            if (freeIpa == null) {
                throw new TestFailException(String.format("'%s' freeIpa cluster was not found for environment '%s'", crn, environmentCrn));
            }
            String name = freeIpa.getName();
            Status status = freeIpa.getStatus();
            throw new TestFailException(String.format("Wait operation timed out, freeIpa '%s' '%s' has been failed for environment '%s'. FreeIpa status: '%s' "
                    + "statusReason: '%s'", name, crn, environmentCrn, status, freeIpa.getStatusReason()));
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Wait operation timed out, freeIpa has been failed. Also failed to get freeIpa status: ")
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
        return String.format("Wait operation was successfully done. '%s' freeIpa is in the desired state '%s'", waitObject.getFreeIpaCrn(),
                waitObject.getDesiredStatus());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String environmentCrn = waitObject.getEnvironmentCrn();
        String crn = waitObject.getFreeIpaCrn();
        try {
            DescribeFreeIpaResponse freeIpa = waitObject.getEndpoint().describe(environmentCrn);
            if (freeIpa == null) {
                LOGGER.info("'{}' freeIpa was not found for environment '{}'. Exit waiting", crn, environmentCrn);
                return false;
            }
            Status status = freeIpa.getStatus();
            if (status == Status.CREATE_FAILED) {
                return false;
            }
            return status.isFailed();
        } catch (ProcessingException clientException) {
            StringBuilder builder = new StringBuilder("Exit waiting! Failed to describe freeIpa cluster due to API client exception: ")
                    .append(System.lineSeparator())
                    .append(clientException.getMessage())
                    .append(System.lineSeparator())
                    .append(clientException);
            LOGGER.error(builder.toString());
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Exit waiting! Exception occurred during describing freeIpa for environment: ")
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
