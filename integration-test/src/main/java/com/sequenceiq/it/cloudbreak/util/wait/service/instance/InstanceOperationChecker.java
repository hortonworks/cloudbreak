package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class InstanceOperationChecker<T extends InstanceWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceOperationChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        List<String> instanceIds = waitObject.getInstanceIds();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        if (waitObject.getInstanceIds().size() > 0) {
            if (actualStatuses.isEmpty()) {
                throw new TestFailException(String.format("'%s' instance was not found.", instanceIds));
            }
            Map<String, String> desiredStatuses = waitObject.getDesiredStatuses();
            LOGGER.info("Waiting for the '{}' state of '{}' instance. Actual state is: '{}'", desiredStatuses, instanceIds, actualStatuses);
            if (waitObject.isDeletionInProgress() || waitObject.isDeleted()) {
                LOGGER.error("Instance '{}' has been getting terminated (status:'{}'), waiting is cancelled.", instanceIds,
                        actualStatuses);
                throw new TestFailException(String.format("Instance '%s' has been getting terminated, waiting is cancelled." +
                        " Status: '%s' statusReason: '%s'", instanceIds, actualStatuses));
            }
            if (waitObject.isFailed()) {
                Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
                LOGGER.error("Instance '{}' is in failed state (status:'{}'), waiting is cancelled.", instanceIds, actualStatuses);
                throw new TestFailException(String.format("Instance '%s' is in failed state. Status: '%s' statusReason: '%s'",
                        instanceIds, actualStatuses, actualStatusReasons));
            }
            if (waitObject.isInDesiredStatus()) {
                LOGGER.info("Instance '{}' is in desired state (status:'{}').", instanceIds, actualStatuses);
                return true;
            }
        } else {
            return true;
        }
        return waitObject.isInDesiredStatus();
    }

    @Override
    public void handleTimeout(T waitObject) {
        List<String> instanceIds = waitObject.getInstanceIds();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        if (actualStatuses.isEmpty()) {
            throw new TestFailException(String.format("'%s' instance was not found.", instanceIds));
        }
        Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
        throw new TestFailException(String.format("Wait operation timed out! Instance '%s' has been failed. Cluster status: '%s' "
                + "statusReason: '%s'", instanceIds, actualStatuses, actualStatusReasons));
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' is in desired ('%s') state.", waitObject.getInstanceIds(),
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
        if (waitObject.isCreateFailed()) {
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            LOGGER.error("Instance '{}' entered into creation failed state (status:'{}'). Exit waiting!", instanceIds, actualStatuses);
            throw new TestFailException(String.format("Instance '%s' entered into creation failed state. Status: '%s' statusReason: '%s'",
                    instanceIds, actualStatuses, actualStatusReasons));
        }
        return waitObject.isInDesiredStatus();
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return waitObject.actualStatuses();
    }

    @Override
    public void refresh(T waitObject) {
        List<String> instanceIds = waitObject.getInstanceIds();
        try {
            waitObject.fetchData();
        } catch (Exception e) {
            LOGGER.error("Failed to get '{}' instance group status, because of {}", instanceIds, e.getMessage(), e);
            throw new TestFailException(String.format("Failed to get '%s' instance group status", instanceIds), e);
        }
    }
}