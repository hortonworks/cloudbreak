package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class WaitFailedChecker<T extends WaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitFailedChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String name = waitObject.getName();
        try {
            waitObject.fetchData();
            Map<String, String> desiredStatuses = waitObject.getDesiredStatuses();
            Map<String, String> actualStatuses = waitObject.actualStatuses();
            LOGGER.info("Waiting for the '{}' state of '{}' cluster. Actual state is: '{}'", desiredStatuses, name, actualStatuses);
            if (waitObject.isDeleted()) {
                Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
                LOGGER.error("Cluster '{}' has been terminated (status:'{}'), waiting is cancelled.", name, actualStatuses);
                throw new TestFailException(String.format("Cluster '%s' has been terminated. Status: '%s' statusReason: '%s'",
                        name, actualStatuses, actualStatusReasons));
            }
            if (waitObject.isInDesiredStatus()) {
                return true;
            }
        } catch (NotFoundException e) {
            LOGGER.warn("No {} found with name: {}", waitObject.getClass().getSimpleName(), name, e);
        } catch (Exception e) {
            LOGGER.error("Failed to get {} status or statusReason: {}", waitObject.getClass().getSimpleName(), e.getMessage(), e);
            throw new TestFailException("Failed to get " + waitObject.getClass().getSimpleName() + " status or statusReason", e);
        }
        return false;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String name = waitObject.getName();
        try {
            Map<String, String> actualStatuses = waitObject.actualStatuses();
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            throw new TestFailException(String.format("Wait operation timed out, '%s' %s has not been failed. Status: '%s' " +
                    "statusReason: '%s'", name, waitObject.getClass().getSimpleName(), actualStatuses, actualStatusReasons));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out! Failed to get {} status or statusReason: {}", waitObject.getClass().getSimpleName(), e.getMessage(), e);
            throw new TestFailException("Wait operation timed out! Failed to get " + waitObject.getClass().getSimpleName() + " status or statusReason", e);
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' %s is in the desired state '%s'", waitObject.getName(),
                waitObject.getClass().getSimpleName(), waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        try {
            String name = waitObject.getName();
            if (waitObject.actualStatuses().isEmpty()) {
                LOGGER.info("'{}' {} was not found. Exit waiting!", waitObject.getClass().getSimpleName(), name);
                return true;
            }
        } catch (ProcessingException clientException) {
            LOGGER.error("Exit waiting! Failed to get cluster due to API client exception: {}", clientException.getMessage(), clientException);
        } catch (Exception e) {
            LOGGER.error("Exit waiting! Failed to get cluster, because of: {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return waitObject.actualStatuses();
    }
}
