package com.sequenceiq.it.cloudbreak.util.wait.service.freeipa.instance;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class FreeIpaInstanceFailedChecker<T extends FreeIpaInstanceWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInstanceFailedChecker.class);

    private boolean failed;

    @Override
    public boolean checkStatus(T waitObject) {
        if (failed) {
            return false;
        }
        List<String> instanceIds = waitObject.getInstanceIds();
        Map<String, String> desiredStatuses = waitObject.getDesiredStatuses();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        LOGGER.info("Waiting for the '{}' state of '{}' instance. Actual state is: '{}'", desiredStatuses, instanceIds, actualStatuses);
        if (waitObject.isDeleted()) {
            Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
            LOGGER.error("Instance '{}' has been terminated (status:'{}'), waiting is cancelled.", instanceIds, actualStatuses);
            throw new TestFailException(String.format("Instance '%s' has been terminated, waiting is cancelled." +
                    " Instance status: '%s' statusReason: '%s'", instanceIds, actualStatuses, actualStatusReasons));
        }
        if (waitObject.isInDesiredStatus()) {
            LOGGER.info("Instance '{}' is in desired state (status:'{}').", instanceIds, actualStatuses);
            return true;
        }
        return waitObject.isInDesiredStatus();
    }

    @Override
    public void handleTimeout(T waitObject) {
        List<String> instanceIds = waitObject.getInstanceIds();
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
        throw new TestFailException(String.format("Wait operation timed out, '%s' %s instances has not been failed. Instance status: '%s' " +
                "statusReason: '%s'", instanceIds, waitObject.getClass().getSimpleName(), actualStatuses, actualStatusReasons));
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' %s is in desired state (status:'%s').", waitObject.getInstanceIds(),
                waitObject.getClass().getSimpleName(), waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        Map<String, String> actualStatuses = waitObject.actualStatuses();
        Map<String, String> actualStatusReasons = waitObject.actualStatusReason();
        List<InstanceMetaDataResponse> instanceMetaDatas = waitObject.getInstanceMetaDatas();
        if (failed) {
            LOGGER.error("Instance '{}' refresh has been failed. Exit waiting!", instanceMetaDatas);
            throw new TestFailException(String.format("Instance '%s' refresh has been failed! Instance status: '%s' statusReason: '%s'", instanceMetaDatas,
                    actualStatuses, actualStatusReasons));
        }
        if (instanceMetaDatas.isEmpty()) {
            LOGGER.error("Instance '{}' was not found. Exit waiting!", instanceMetaDatas);
            throw new TestFailException(String.format("Instance '%s' was not found!", instanceMetaDatas));
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
            failed = false;
        } catch (Exception e) {
            LOGGER.error("Failed to get instances: '{}', because of {}", instanceIds, e.getMessage(), e);
            throw new TestFailException(String.format("Failed to get instances status: '%s'", instanceIds), e);
        }
    }
}