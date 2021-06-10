package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class InstanceFailedChecker<T extends InstanceWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceFailedChecker.class);

    private boolean failed;

    @Override
    public boolean checkStatus(T waitObject) {
        if (failed) {
            return false;
        }
        String name = waitObject.getHostGroup();
        Map<String, String> desiredStatuses = waitObject.getDesiredStatuses();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        LOGGER.info("Waiting for the '{}' state of '{}' instance. Actual state is: '{}'", desiredStatuses, name, actualStatuses);
        if (waitObject.isDeleted()) {
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            LOGGER.error("Instance '{}' has been terminated (status:'{}'), waiting is cancelled.", name, actualStatuses);
            throw new TestFailException(String.format("Instance '%s' has been terminated, waiting is cancelled." +
                    " Instance status: '%s' statusReason: '%s'", name, actualStatuses, actualStatusReasons));
        }
        if (waitObject.isInDesiredStatus()) {
            LOGGER.info("Instance '{}' is in desired state (status:'{}').", name, actualStatuses);
            return true;
        }
        return waitObject.isInDesiredStatus();
    }

    @Override
    public void handleTimeout(T waitObject) {
        String name = waitObject.getHostGroup();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
        throw new TestFailException(String.format("Wait operation timed out, '%s' %s instance group has not been failed. Instance status: '%s' " +
                "statusReason: '%s'", name, waitObject.getClass().getSimpleName(), actualStatuses, actualStatusReasons));
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' %s is in desired state (status:'%s').", waitObject.getHostGroup(),
                waitObject.getClass().getSimpleName(), waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String name = waitObject.getHostGroup();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
        Optional<InstanceGroupV4Response> instanceGroup = waitObject.getInstanceGroup();
        if (failed) {
            LOGGER.error("Instance '{}' refresh has been failed. Exit waiting!", name);
            throw new TestFailException(String.format("Instance '%s' refresh has been failed! Instance status: '%s' statusReason: '%s'", name, actualStatuses,
                    actualStatusReasons));
        }
        if (instanceGroup.isEmpty()) {
            LOGGER.error("Instance '{}' was not found. Exit waiting!", name);
            throw new TestFailException(String.format("Instance '%s' was not found!", name));
        }
        return waitObject.isInDesiredStatus();
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return waitObject.actualStatuses();
    }

    @Override
    public void refresh(T waitObject) {
        String name = waitObject.getHostGroup();
        try {
            waitObject.fetchData();
            failed = false;
        } catch (NoSuchElementException e) {
            LOGGER.warn("No instance group found with name '{}'", name, e);
            failed = true;
        } catch (Exception e) {
            LOGGER.error("Failed to get instance group status: '{}', because of {}", name, e.getMessage(), e);
            throw new TestFailException(String.format("Failed to get instance group status: '%s'", name), e);
        }
    }
}