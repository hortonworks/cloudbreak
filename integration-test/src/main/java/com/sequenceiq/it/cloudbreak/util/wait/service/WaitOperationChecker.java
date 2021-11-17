package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class WaitOperationChecker<T extends WaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitOperationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        String name = waitObject.getName();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        if (actualStatuses.isEmpty()) {
            throw new TestFailException(String.format("'%s' stack was not found.", name));
        }
        Map<String, String> desiredStatuses = waitObject.getDesiredStatuses();
        LOGGER.info("Waiting for the '{}' state of '{}' cluster. Actual state is: '{}'", desiredStatuses, name, actualStatuses);
        if (waitObject.isDeletionInProgress() || waitObject.isDeleted()) {
            LOGGER.error("Cluster '{}' has been getting terminated (status:'{}'), waiting is cancelled.", name, actualStatuses);
            throw new TestFailException(String.format("Cluster '%s' has been getting terminated (status:'%s'), waiting is cancelled.", name,
                    actualStatuses));
        }
        if (waitObject.isInDesiredStatus()) {
            LOGGER.info("Cluster '{}' is in desired state (status:'{}').", name, actualStatuses);
            return true;
        }
        if (waitObject.isFailed()) {
            handleFailedState(waitObject, name, actualStatuses);
        }

        return waitObject.isInDesiredStatus();
    }

    private void handleFailedState(T waitObject, String name, Map<String, String> actualStatuses) {
        if (waitObject.isFailedButIgnored()) {
            LOGGER.info("Cluster '{}' is in a failed state but the test will ignore it (status:'{}').", name, actualStatuses);
        } else {
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            LOGGER.error("Cluster '{}' is in failed state (status:'{}'), waiting is cancelled.", name, actualStatuses);
            throw new TestFailException(String.format("Cluster '%s' is in failed state. Status: '%s' statusReason: '%s'",
                    name, actualStatuses, actualStatusReasons));
        }
    }

    @Override
    public void handleTimeout(T waitObject) {
        String name = waitObject.getName();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        if (actualStatuses.isEmpty()) {
            throw new TestFailException(String.format("'%s' cluster was not found.", name));
        }
        Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
        throw new TestFailException(String.format("Wait operation timed out! Cluster '%s' has been failed. Cluster status: '%s' "
                + "statusReason: '%s'", name, actualStatuses, actualStatusReasons));
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' cluster is in the desired state '%s'",
                waitObject.getName(), waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getName();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        if (actualStatuses.isEmpty()) {
            LOGGER.info("'{}' cluster was not found. Exit waiting!", name);
            return true;
        }
        if (waitObject.isCreateFailed()) {
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            LOGGER.error("Cluster '{}' entered into creation failed state (status:'{}'). Exit waiting!", name, actualStatuses);
            throw new TestFailException(String.format("Cluster '%s' entered into creation failed state. Status: '%s' statusReason: '%s'",
                    name, actualStatuses, actualStatusReasons));
        }
        return waitObject.isInDesiredStatus();
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return waitObject.actualStatuses();
    }

    @Override
    public void refresh(T waitObject) {
        try {
            waitObject.fetchData();
        } catch (Exception e) {
            LOGGER.error("Failed to get cluster status or statusReason: {}", e.getMessage(), e);
            throw new TestFailException("Failed to get cluster status or statusReason", e);
        }
    }
}
