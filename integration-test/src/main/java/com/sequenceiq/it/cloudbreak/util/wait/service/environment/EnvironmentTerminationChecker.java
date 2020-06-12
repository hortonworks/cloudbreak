package com.sequenceiq.it.cloudbreak.util.wait.service.environment;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ARCHIVED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.DELETE_FAILED;

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
            if (environment.getEnvironmentStatus() == DELETE_FAILED) {
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
            throw new TestFailException(String.format("Environment termination failed: %s", e.getMessage()));
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
            StringBuilder builder = new StringBuilder("Wait operation timed out, environment termination failed. Also failed to get environment status: ")
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
        return String.format("'%s' environment termination successfully finished.", waitObject.getCrn());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String crn = waitObject.getCrn();
        try {
            DetailedEnvironmentResponse environment = waitObject.getEndpoint().getByCrn(crn);
            EnvironmentStatus status = environment.getEnvironmentStatus();
            if (status == DELETE_FAILED) {
                return false;
            }
            return status.isFailed();
        } catch (ProcessingException clientException) {
            StringBuilder builder = new StringBuilder("Exit waiting! Failed to get environment due to API client exception: ")
                    .append(System.lineSeparator())
                    .append(clientException.getMessage())
                    .append(System.lineSeparator())
                    .append(clientException);
            LOGGER.error(builder.toString());
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder("Exit waiting! Failed to get environment, because of: ")
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
