package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.util.Map;

import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class WaitFailedChecker<T extends WaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitFailedChecker.class);

    private boolean failed;

    @Override
    public boolean checkStatus(T waitObject) {
        if (failed) {
            return false;
        }
        String name = waitObject.getName();
        Map<String, String> desiredStatuses = waitObject.getDesiredStatuses();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        LOGGER.info("Waiting for the '{}' state of '{}' cluster. Actual state is: '{}'", desiredStatuses, name, actualStatuses);
        if (waitObject.isDeleted()) {
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            LOGGER.error("Cluster '{}' has been terminated (status:'{}'), waiting is cancelled.", name, actualStatuses);
            throw new TestFailException(String.format("Cluster '%s' has been terminated. Status: '%s' statusReason: '%s'",
                    name, actualStatuses, actualStatusReasons));
        }
        return waitObject.isInDesiredStatus();
    }

    @Override
    public void handleTimeout(T waitObject) {
        String name = waitObject.getName();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
        throw new TestFailException(String.format("Wait operation timed out, '%s' %s has not been failed. Status: '%s' " +
                "statusReason: '%s'", name, waitObject.getClass().getSimpleName(), actualStatuses, actualStatusReasons));
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' %s is in the desired state '%s'", waitObject.getName(),
                waitObject.getClass().getSimpleName(), waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getName();
        if (failed || waitObject.actualStatuses().isEmpty()) {
            LOGGER.info("'{}' {} was not found. Exit waiting!", waitObject.getClass().getSimpleName(), name);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return waitObject.actualStatuses();
    }

    @Override
    public void refresh(T waitObject) {
        try {
            waitObject.fetchData();
            failed = false;
        } catch (NotFoundException e) {
            LOGGER.warn("No {} found with name: {}", waitObject.getClass().getSimpleName(), waitObject.getName(), e);
            failed = true;
        } catch (Exception e) {
            LOGGER.error("Failed to get {} status or statusReason: {}", waitObject.getClass().getSimpleName(), e.getMessage(), e);
            throw new TestFailException("Failed to get " + waitObject.getClass().getSimpleName() + " status or statusReason", e);
        }
    }
}
