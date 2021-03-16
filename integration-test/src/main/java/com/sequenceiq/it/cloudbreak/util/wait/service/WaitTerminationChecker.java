package com.sequenceiq.it.cloudbreak.util.wait.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;

import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class WaitTerminationChecker<T extends WaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitTerminationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        Map<String, String> desiredStatuses = waitObject.getDesiredStatuses();
        String name = waitObject.getName();
        try {
            waitObject.fetchData();
            Map<String, String> actualStatuses = waitObject.actualStatuses();
            LOGGER.info("Waiting for the '{}' state of '{}' cluster. Actual state is: '{}'", desiredStatuses, name, actualStatuses);
            if (waitObject.isDeleteFailed()) {
                Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
                LOGGER.error("Cluster '{}' termination failed (status:'{}'), waiting is cancelled.", name, actualStatuses);
                throw new TestFailException(String.format("Cluster '%s' termination failed. Status: '%s' statusReason: '%s'",
                        name, actualStatuses, actualStatusReasons));
            }
            if (!waitObject.isDeleted()) {
                return false;
            }
        } catch (NotFoundException e) {
            LOGGER.warn("No {} found with name: {}", waitObject.getClass().getSimpleName(), name);
        } catch (Exception e) {
            LOGGER.error("Cluster termination failed, because of: {}", e.getMessage(), e);
            throw new TestFailException("Cluster termination failed", e);
        }
        return true;
    }

    @Override
    public void handleTimeout(T waitObject) {
        try {
            String name = waitObject.getName();
            Map<String, String> actualStatuses = waitObject.actualStatuses();
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            throw new TestFailException(String.format("Wait operation timed out! '%s' cluster termination failed. Cluster status: '%s' " +
                    "statusReason: '%s'", name, actualStatuses, actualStatusReasons));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out! Failed to get cluster status or statusReason: {}", e.getMessage(), e);
            throw new TestFailException("Wait operation timed out! Failed to get cluster status or statusReason", e);
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("'%s' cluster termination successfully finished.", waitObject.getName());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        try {
            if (waitObject.isDeleteFailed()) {
                return false;
            }
            return waitObject.isFailed();
        } catch (ProcessingException clientException) {
            LOGGER.error("Exit waiting! Failed to get cluster due to API client exception: {}", clientException.getMessage(), clientException);
        } catch (Exception e) {
            LOGGER.warn("Exit waiting! Failed to get cluster, because of: {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        String name = waitObject.getName();
        try {
            return waitObject.actualStatuses();
        } catch (NotFoundException e) {
            LOGGER.warn("No cluster found with name '{}'! It has been deleted successfully.", name, e);
            return Map.of("status", DELETE_COMPLETED.name());
        }
    }
}
