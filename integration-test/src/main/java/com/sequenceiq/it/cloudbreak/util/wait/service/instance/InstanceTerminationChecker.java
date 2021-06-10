package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class InstanceTerminationChecker<T extends InstanceWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationChecker.class);

    private boolean deleted;

    @Override
    public boolean checkStatus(T waitObject) {
        if (deleted) {
            return true;
        }
        Map<String, String> desiredStatuses = waitObject.getDesiredStatuses();
        String name = waitObject.getHostGroup();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        LOGGER.info("Waiting for the '{}' state of '{}' instance. Actual state is: '{}'", desiredStatuses, name, actualStatuses);
        if (waitObject.isDeleteFailed()) {
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            LOGGER.error("Instance '{}' termination failed (status:'{}'), waiting is cancelled.", name, actualStatuses);
            throw new TestFailException(String.format("Instance '%s' termination failed. Status: '%s' statusReason: '%s'",
                    name, actualStatuses, actualStatusReasons));
        }
        return waitObject.isDeleted();
    }

    @Override
    public void handleTimeout(T waitObject) {
        String name = waitObject.getHostGroup();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
        throw new TestFailException(String.format("Wait operation timed out! '%s' instance termination failed. Cluster status: '%s' " +
                "statusReason: '%s'", name, actualStatuses, actualStatusReasons));
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' is in desired ('%s') state.'", waitObject.getHostGroup(),
                waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getHostGroup();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        if (actualStatuses.isEmpty()) {
            LOGGER.info("'{}' instance was not found. Exit waiting!", name);
            return true;
        }
        if (waitObject.isDeleteFailed()) {
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            LOGGER.error("Instance '{}' termination failed (status:'{}'). Exit waiting!", name, actualStatuses);
            throw new TestFailException(String.format("Instance '%s' termination failed. Status: '%s' statusReason: '%s'",
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
        String name = waitObject.getHostGroup();
        try {
            waitObject.fetchData();
        } catch (NoSuchElementException e) {
            LOGGER.warn("{} instance group is not present, may this was deleted.", name, e);
            deleted = true;
        } catch (Exception e) {
            LOGGER.error("'{}' instance group deletion has been failed, because of: {}", name, e.getMessage(), e);
            throw new TestFailException(String.format("'%s' instance group deletion has been failed", name), e);
        }
    }
}