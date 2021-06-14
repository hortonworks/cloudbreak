package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import java.util.List;
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
        List<String> instanceIds = waitObject.getInstanceIds();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        LOGGER.info("Waiting for the '{}' state of '{}' instance. Actual state is: '{}'", desiredStatuses, instanceIds, actualStatuses);
        if (waitObject.isDeleteFailed()) {
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            LOGGER.error("Instance '{}' termination failed (status:'{}'), waiting is cancelled.", instanceIds, actualStatuses);
            throw new TestFailException(String.format("Instance '%s' termination failed. Status: '%s' statusReason: '%s'",
                    instanceIds, actualStatuses, actualStatusReasons));
        }
        return waitObject.isDeleted();
    }

    @Override
    public void handleTimeout(T waitObject) {
        List<String> instanceIds = waitObject.getInstanceIds();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
        throw new TestFailException(String.format("Wait operation timed out! '%s' instance termination failed. Cluster status: '%s' " +
                "statusReason: '%s'", instanceIds, actualStatuses, actualStatusReasons));
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' is in desired ('%s') state.'", waitObject.getInstanceIds(),
                waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        List<String> instanceIds = waitObject.getInstanceIds();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        if (actualStatuses.isEmpty()) {
            LOGGER.info("'{}' instance was not found. Exit waiting!", instanceIds);
            return true;
        }
        if (waitObject.isDeleteFailed()) {
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            LOGGER.error("Instance '{}' termination failed (status:'{}'). Exit waiting!", instanceIds, actualStatuses);
            throw new TestFailException(String.format("Instance '%s' termination failed. Status: '%s' statusReason: '%s'",
                    instanceIds, actualStatuses, actualStatusReasons));
        }
        return waitObject.isDeleted();
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return deleted ? waitObject.getDesiredStatuses() : waitObject.actualStatuses();
    }

    @Override
    public void refresh(T waitObject) {
        List<String> instanceIds = waitObject.getInstanceIds();
        try {
            waitObject.fetchData();
        } catch (NoSuchElementException e) {
            LOGGER.warn("{} instance id is not present, may this was deleted.", instanceIds, e);
            deleted = true;
        } catch (Exception e) {
            LOGGER.error("'{}' instance id deletion has been failed, because of: {}", instanceIds, e.getMessage(), e);
            throw new TestFailException(String.format("'%s' id group deletion has been failed", instanceIds), e);
        }
    }
}