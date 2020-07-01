package com.sequenceiq.it.cloudbreak.util.wait.service.redbeams;

import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_FAILED;

import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;

public class RedbeamsTerminationChecker<T extends RedbeamsWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsTerminationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String crn = waitObject.getCrn();
        Status desiredStatus = waitObject.getDesiredStatus();
        try {
            DatabaseServerV4Response redbeams = waitObject.getEndpoint().getByCrn(crn);
            String name = redbeams.getName();
            Status status = redbeams.getStatus();
            LOGGER.info("Waiting for the '{}' state of '{}' '{}' redbeams. Actual state is: '{}'", desiredStatus, name, crn, status);
            if (redbeams.getStatus().equals(DELETE_FAILED)) {
                LOGGER.error("Redbeams '{}' '{}' termination failed (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Redbeams '%s' '%s' termination failed. Status: '%s' statusReason: '%s'",
                        name, crn, status, redbeams.getStatusReason()));
            }
            if (!status.equals(DELETE_COMPLETED)) {
                return false;
            }
        } catch (NotFoundException e) {
            LOGGER.warn("No DatabaseServerConfig found with crn '{}'", crn, e);
        } catch (Exception e) {
            LOGGER.error("Redbeams termination failed: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Redbeams termination failed: ", e.getMessage()));
        }
        return true;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DatabaseServerV4Response redbeams = waitObject.getEndpoint().getByCrn(crn);
            throw new TestFailException(String.format("Wait operation timed out, '%s' '%s' redbeams termination failed. Redbeams status: '%s' " +
                    "statusReason: '%s'", redbeams.getName(), crn, redbeams.getStatus(), redbeams.getStatusReason()));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out, redbeams termination failed. Also failed to get redbeams status: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Wait operation timed out, redbeams termination failed. Also failed to get redbeams status: %s",
                    e.getMessage()));
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("'%s' redbeams termination successfully finished.", waitObject.getCrn());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DatabaseServerV4Response redbeams = waitObject.getEndpoint().getByCrn(crn);
            Status status = redbeams.getStatus();
            if (status.equals(DELETE_FAILED)) {
                return true;
            }
            return waitObject.isFailed(status);
        } catch (ProcessingException clientException) {
            LOGGER.error("Exit waiting! Failed to get redbeams due to API client exception: {}", clientException.getMessage(), clientException);
        } catch (Exception e) {
            LOGGER.error("Exit waiting! Failed to get redbeams, because of: {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DatabaseServerV4Response redbeams = waitObject.getEndpoint().getByCrn(crn);
            return Map.of("status", redbeams.getStatus().name());
        } catch (NotFoundException e) {
            LOGGER.warn("No DatabaseServerConfig found with crn '{}'", crn, e);
            return Map.of("status", DELETE_COMPLETED.name());
        }
    }
}
