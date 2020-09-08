package com.sequenceiq.it.cloudbreak.util.wait.service.redbeams;

import static com.sequenceiq.redbeams.api.model.common.Status.CREATE_FAILED;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.redbeams.api.model.common.Status.DELETE_REQUESTED;
import static com.sequenceiq.redbeams.api.model.common.Status.PRE_DELETE_IN_PROGRESS;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;

public class RedbeamsOperationChecker<T extends RedbeamsWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsOperationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String crn = waitObject.getCrn();
        Status desiredStatus = waitObject.getDesiredStatus();
        try {
            DatabaseServerV4Response redbeams = waitObject.getEndpoint().getByCrn(crn);
            if (redbeams == null) {
                throw new TestFailException(String.format("'%s' redbeams was not found.", crn));
            }
            String name = redbeams.getName();
            Status status = redbeams.getStatus();
            LOGGER.info("Waiting for the '{}' state of '{}' '{}' redbeams. Actual state is: '{}'", desiredStatus, name, crn, status);
            if (isDeletionInProgress(status) || status.equals(DELETE_COMPLETED)) {
                LOGGER.error("Redbeams '{}' '{}' has been getting terminated (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Redbeams '%s' '%s' has been getting terminated (status:'%s'), waiting is cancelled.", name, crn,
                        status));
            }
            if (waitObject.isFailed(status)) {
                LOGGER.error("Redbeams '{}' '{}' is in failed state (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Redbeams '%s' '%s' is in failed state. Status: '%s' statusReason: '%s'",
                        name, crn, status, redbeams.getStatusReason()));
            }
            if (desiredStatus.equals(status)) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Redbeams has been failed. Also failed to get redbeams status: {}", e.getMessage(), e);
            throw new TestFailException("Redbeams has been failed. Also failed to get redbeams status", e);
        }
        return false;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DatabaseServerV4Response redbeams = waitObject.getEndpoint().getByCrn(crn);
            if (redbeams == null) {
                throw new TestFailException(String.format("'%s' redbeams was not found.", crn));
            }
            String name = redbeams.getName();
            Status status = redbeams.getStatus();
            throw new TestFailException(String.format("Wait operation timed out, redbeams '%s' '%s' has been failed. Redbeams status: '%s' "
                    + "statusReason: '%s'", name, crn, status, redbeams.getStatusReason()));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out, redbeams has been failed. Also failed to get redbeams status: {}", e.getMessage(), e);
            throw new TestFailException("Wait operation timed out, redbeams has been failed. Also failed to get redbeams status", e);
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' redbeams is in the desired state '%s'",
                waitObject.getCrn(), waitObject.getDesiredStatus());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DatabaseServerV4Response redbeams = waitObject.getEndpoint().getByCrn(crn);
            if (redbeams == null) {
                LOGGER.info("'{}' redbeams was not found. Exit waiting!", crn);
                return true;
            }
            Status status = redbeams.getStatus();
            if (status.equals(CREATE_FAILED)) {
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
        return Map.of("status", waitObject.getEndpoint().getByCrn(waitObject.getCrn())
                .getStatus().name());
    }

    private boolean isDeletionInProgress(Status redbeamsStatus) {
        Set<Status> deleteInProgressStatuses = Set.of(DELETE_REQUESTED, PRE_DELETE_IN_PROGRESS, DELETE_IN_PROGRESS);
        return deleteInProgressStatuses.contains(redbeamsStatus);
    }
}
