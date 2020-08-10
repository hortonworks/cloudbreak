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
            LOGGER.error("FreeIpa creation failed. Also failed to get freeIpa status: {}", e.getMessage(), e);
            throw new TestFailException(String.format("FreeIpa creation failed. Also failed to get freeIpa status: %s", e.getMessage()));
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
            LOGGER.error("Wait operation timed out, freeIpa has been failed. Also failed to get freeIpa status: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Wait operation timed out, freeIpa has been failed. Also failed to get freeIpa status: %s",
                    e.getMessage()));
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
