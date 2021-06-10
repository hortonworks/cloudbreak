package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.util.Map;

import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class WaitTerminationChecker<T extends WaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaitTerminationChecker.class);

    private boolean deleted;

    @Override
    public boolean checkStatus(T waitObject) {
        if (deleted) {
            return true;
        }
        Map<String, String> desiredStatuses = waitObject.getDesiredStatuses();
        String name = waitObject.getName();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        LOGGER.info("Waiting for the '{}' state of '{}' cluster. Actual state is: '{}'", desiredStatuses, name, actualStatuses);
        if (waitObject.isDeleteFailed()) {
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            LOGGER.error("Cluster '{}' termination failed (status:'{}'), waiting is cancelled.", name, actualStatuses);
            throw new TestFailException(String.format("Cluster '%s' termination failed. Status: '%s' statusReason: '%s'",
                    name, actualStatuses, actualStatusReasons));
        }
        return waitObject.isDeleted();
    }

    @Override
    public void handleTimeout(T waitObject) {
        String name = waitObject.getName();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
        throw new TestFailException(String.format("Wait operation timed out! '%s' cluster termination failed. Cluster status: '%s' " +
                "statusReason: '%s'", name, actualStatuses, actualStatusReasons));
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("'%s' cluster termination successfully finished.", waitObject.getName());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getName();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        if (actualStatuses.isEmpty()) {
            LOGGER.info("'{}' cluster was not found. Exit waiting!", name);
            return true;
        }
        if (waitObject.isDeleteFailed()) {
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            LOGGER.error("Cluster '{}' termination failed (status:'{}'). Exit waiting!", name, actualStatuses);
            throw new TestFailException(String.format("Cluster '%s' termination failed. Status: '%s' statusReason: '%s'",
                    name, actualStatuses, actualStatusReasons));
        }
        return waitObject.isDeleted();
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return deleted ? waitObject.getDesiredStatuses() : waitObject.actualStatuses();
    }

    @Override
    public void refresh(T waitObject) {
        String name = waitObject.getName();
        try {
            waitObject.fetchData();
        } catch (NotFoundException e) {
            LOGGER.warn("No {} found with name: {}", waitObject.getClass().getSimpleName(), name);
            deleted = true;
        } catch (Exception e) {
            LOGGER.error("Cluster termination failed, because of: {}", e.getMessage(), e);
            throw new TestFailException("Cluster termination failed", e);
        }
    }
}
