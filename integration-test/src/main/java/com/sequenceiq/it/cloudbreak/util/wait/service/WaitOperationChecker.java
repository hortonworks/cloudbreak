package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.util.Map;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class WaitOperationChecker<T extends WaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitOperationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        try {
            waitObject.fetchData();
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
            if (waitObject.isFailed()) {
                Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
                LOGGER.error("Cluster '{}' is in failed state (status:'{}'), waiting is cancelled.", name, actualStatuses);
                throw new TestFailException(String.format("Cluster '%s' is in failed state. Status: '%s' statusReason: '%s'",
                        name, actualStatuses, actualStatusReasons));
            }
            if (waitObject.isInDesiredStatus()) {
                LOGGER.info("Cluster '{}' is in desired state (status:'{}').", name, actualStatuses);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get cluster status or statusReason: {}", e.getMessage(), e);
            throw new TestFailException("Failed to get cluster status or statusReason", e);
        }
        return false;
    }

    @Override
    public void handleTimeout(T waitObject) {
        try {
            String name = waitObject.getName();
            Map<String, String> actualStatuses = waitObject.actualStatuses();
            if (actualStatuses.isEmpty()) {
                throw new TestFailException(String.format("'%s' cluster was not found.", name));
            }
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            throw new TestFailException(String.format("Wait operation timed out! Cluster '%s' has been failed. Cluster status: '%s' "
                    + "statusReason: '%s'", name, actualStatuses, actualStatusReasons));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out! Failed to get cluster status or statusReason: {}", e.getMessage(), e);
            throw new TestFailException("Wait operation timed out! Failed to get cluster status or statusReason", e);
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' cluster is in the desired state '%s'",
                waitObject.getName(), waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        try {
            String name = waitObject.getName();
            Map<String, String> actualStatuses = waitObject.actualStatuses();
            if (actualStatuses.isEmpty()) {
                LOGGER.info("'{}' cluster was not found. Exit waiting!", name);
                return true;
            }
            if (waitObject.isCreateFailed()) {
                LOGGER.info("'{}' the polled resource entered into creation failed state. Exit waiting!", name);
                return true;
            }
            return waitObject.isFailed();
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
