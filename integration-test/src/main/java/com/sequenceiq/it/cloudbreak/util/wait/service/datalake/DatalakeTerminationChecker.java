package com.sequenceiq.it.cloudbreak.util.wait.service.datalake;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETE_FAILED;

import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DatalakeTerminationChecker<T extends DatalakeWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeTerminationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String name = waitObject.getName();
        SdxClusterStatusResponse desiredStatus = waitObject.getDesiredStatus();
        try {
            SdxClusterResponse sdx = waitObject.getEndpoint().get(name);
            String crn = sdx.getCrn();
            SdxClusterStatusResponse status = sdx.getStatus();
            LOGGER.info("Waiting for the '{}' state of '{}' '{}' datalake. Actual state is: '{}'", desiredStatus, name, crn, status);
            if (sdx.getStatus().equals(DELETE_FAILED)) {
                LOGGER.error("Datalake '{}' '{}' termination failed (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Datalake '%s' '%s' termination failed. Status: '%s' statusReason: '%s'",
                        name, crn, status, sdx.getStatusReason()));
            }
            if (!status.equals(DELETED)) {
                return false;
            }
        } catch (NotFoundException e) {
            LOGGER.warn("No datalake found with name '{}'", name, e);
        } catch (Exception e) {
            LOGGER.error("Datalake termination failed: {}", e.getMessage(), e);
            throw new TestFailException("Datalake termination failed", e);
        }
        return true;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String name = waitObject.getName();
        try {
            SdxClusterResponse sdx = waitObject.getEndpoint().get(name);
            throw new TestFailException(String.format("Wait operation timed out, '%s' '%s' datalake termination failed. Datalake status: '%s' " +
                    "statusReason: '%s'", name, sdx.getCrn(), sdx.getStatus(), sdx.getStatusReason()));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out, datalake termination failed. Also failed to get datalake status: {}", e.getMessage(), e);
            throw new TestFailException("Wait operation timed out, datalake termination failed. Also failed to get datalake status", e);
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("'%s' datalake termination successfully finished.", waitObject.getName());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getName();
        try {
            SdxClusterResponse sdx = waitObject.getEndpoint().get(name);
            SdxClusterStatusResponse status = sdx.getStatus();
            if (status.equals(DELETE_FAILED)) {
                return false;
            }
            return waitObject.isFailed(status);
        } catch (ProcessingException clientException) {
            LOGGER.error("Exit waiting! Failed to get datalake due to API client exception: {}", clientException.getMessage(), clientException);
        } catch (Exception e) {
            LOGGER.error("Exit waiting! Failed to get datalake, because of: {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        String name = waitObject.getName();
        try {
            SdxClusterResponse sdx = waitObject.getEndpoint().get(name);
            return Map.of("status", sdx.getStatus().name());
        } catch (NotFoundException e) {
            LOGGER.warn("No datalake found with name '{}'! It has been deleted successfully.", name, e);
            return Map.of("status", DELETED.name());
        }
    }
}
