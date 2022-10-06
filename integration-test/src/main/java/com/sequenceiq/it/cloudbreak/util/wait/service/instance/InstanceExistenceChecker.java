package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.UmsTimeoutWorkaroundUtils;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class InstanceExistenceChecker<T extends InstanceWaitObject> extends ExceptionChecker<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceExistenceChecker.class);

    private static final Map<String, String> EMPTY_STATUS_MAP = Map.of();

    @Override
    public boolean checkStatus(T waitObject) {
        return doInstancesExist(waitObject);
    }

    @Override
    public void handleTimeout(T waitObject) {
        if (!doInstancesExist(waitObject)) {
            throw new TestFailException("Instances were not found.");
        }
        throw new TestFailException("Wait operation timed out, yet existing instances were found. This should not happen!");
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation has successfully finished. Instances now exist with the following IDs: %s.",
                waitObject.getFetchedInstanceIds());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return EMPTY_STATUS_MAP;
    }

    @Override
    public void refresh(T waitObject) {
        try {
            waitObject.fetchData();
        } catch (Exception e) {
            if (!UmsTimeoutWorkaroundUtils.umsTimeoutRelatedException(e)) {
                LOGGER.error("Failed to get instance group status, because of {}", e.getMessage(), e);
                throw new TestFailException("Failed to get instance group status", e);
            }
        }
    }

    private boolean doInstancesExist(T waitObject) {
        List<String> fetchedInstanceIds = waitObject.getFetchedInstanceIds();
        return !(fetchedInstanceIds.isEmpty()) && fetchedInstanceIds.stream().noneMatch(Objects::isNull);
    }
}
