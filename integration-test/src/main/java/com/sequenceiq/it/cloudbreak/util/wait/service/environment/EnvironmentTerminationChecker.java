package com.sequenceiq.it.cloudbreak.util.wait.service.environment;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_FAILED;

import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class EnvironmentTerminationChecker<T extends EnvironmentWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentTerminationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String crn = waitObject.getCrn();
        EnvironmentStatus desiredStatus = waitObject.getDesiredStatus();
        try {
            DetailedEnvironmentResponse environment = waitObject.getEndpoint().getByCrn(crn);
            String name = environment.getName();
            EnvironmentStatus status = environment.getEnvironmentStatus();
            LOGGER.info("Waiting for the '{}' state of '{}' '{}' environment. Actual state is: '{}'", desiredStatus, name, crn, status);
            if (environment.getEnvironmentStatus().equals(DELETE_FAILED)) {
                LOGGER.error("Environment '{}' '{}' termination failed (status:'{}'), waiting is cancelled.", name, crn, status);
                throw new TestFailException(String.format("Environment '%s' '%s' termination failed. Status: '%s' statusReason: '%s'",
                        name, crn, status, environment.getStatusReason()));
            }
            if (!status.equals(ARCHIVED)) {
                return false;
            }
        } catch (NotFoundException e) {
            LOGGER.warn("No environment found with crn '{}'", crn, e);
        } catch (Exception e) {
            LOGGER.error("Environment termination failed: {}", e.getMessage(), e);
            throw new TestFailException("Environment termination failed", e);
        }
        return true;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DetailedEnvironmentResponse environment = waitObject.getEndpoint().getByCrn(crn);
            throw new TestFailException(String.format("Wait operation timed out, '%s' '%s' environment termination failed. Environment status: '%s' " +
                    "statusReason: '%s'", environment.getName(), crn, environment.getEnvironmentStatus(), environment.getStatusReason()));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out, environment termination failed. Also failed to get environment status: {}", e.getMessage(), e);
            throw new TestFailException("Wait operation timed out, environment termination failed. Also failed to get environment status", e);
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("'%s' environment termination successfully finished.", waitObject.getCrn());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DetailedEnvironmentResponse environment = waitObject.getEndpoint().getByCrn(crn);
            EnvironmentStatus status = environment.getEnvironmentStatus();
            if (status.equals(DELETE_FAILED)) {
                return false;
            }
            return status.isFailed();
        } catch (ProcessingException clientException) {
            LOGGER.error("Exit waiting! Failed to get environment due to API client exception: {}", clientException.getMessage(), clientException);
        } catch (Exception e) {
            LOGGER.error("Exit waiting! Failed to get environment, because of: {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DetailedEnvironmentResponse environment = waitObject.getEndpoint().getByCrn(crn);
            return Map.of("status", environment.getEnvironmentStatus().name());
        } catch (NotFoundException e) {
            LOGGER.warn("No environment found with crn '{}'! It has been deleted successfully.", crn, e);
            return Map.of("status", ARCHIVED.name());
        }
    }
}
