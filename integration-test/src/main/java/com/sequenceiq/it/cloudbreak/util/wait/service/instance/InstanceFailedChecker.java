package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.ws.rs.ProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.wait.service.ExceptionChecker;

public class InstanceFailedChecker<T extends InstanceWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceFailedChecker.class);

    @Override
    public boolean checkStatus(T waitObject) {
        Map<String, String> desiredStatuses = waitObject.getDesiredStatuses();
        String hostGroup = waitObject.getHostGroup();
        try {
            waitObject.fetchData();
            InstanceMetaDataV4Response instanceMetaDataV4Response = waitObject.getInstanceMetadata();
            InstanceStatus instanceStatus = instanceMetaDataV4Response.getInstanceStatus();
            String instanceGroupName = instanceMetaDataV4Response.getInstanceGroup();
            String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
            LOGGER.info("Waiting for the '{}'. Actual instance state is: '{}'", desiredStatuses, instanceStatus);
            if (waitObject.isDeleted()) {
                LOGGER.error("The '{}' has been terminated (status:'{}'), waiting is cancelled.", instanceGroupName, instanceStatus);
                throw new TestFailException(String.format("The '%s' has been terminated, waiting is cancelled." +
                        " Status: '%s' statusReason: '%s'", instanceGroupName, instanceStatus, hostStatusReason));
            }
            if (waitObject.isInDesiredStatus()) {
                LOGGER.info("The '{}' is in desired state (status:'{}').", instanceGroupName, instanceStatus);
                return true;
            }
        } catch (NoSuchElementException e) {
            LOGGER.warn("No instance group found with name '{}'", hostGroup, e);
        } catch (Exception e) {
            LOGGER.error("Failed to get instance group status: '{}', because of {}", hostGroup, e.getMessage(), e);
            throw new TestFailException(String.format("Failed to get instance group status: '%s'", hostGroup), e);
        }
        return false;
    }

    @Override
    public void handleTimeout(T waitObject) {
        try {
            InstanceMetaDataV4Response instanceMetaDataV4Response = waitObject.getInstanceMetadata();
            InstanceStatus instanceStatus = instanceMetaDataV4Response.getInstanceStatus();
            String instanceGroupName = instanceMetaDataV4Response.getInstanceGroup();
            String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
            throw new TestFailException(String.format("Wait operation timed out, '%s' instance group has not been failed. Instance status: '%s' " +
                    "statusReason: '%s'", instanceGroupName, instanceStatus, hostStatusReason));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out! Failed to get instance status: {}", e.getMessage(), e);
            throw new TestFailException("Wait operation timed out! Failed to get instance status", e);
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' is in desired ('%s') state.", waitObject.getHostGroup(),
                waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String hostGroup = waitObject.getHostGroup();
        try {
            Optional<InstanceGroupV4Response> instanceGroup = waitObject.getInstanceGroup();
            if (instanceGroup.isEmpty()) {
                LOGGER.info("'{}' instance group was not found. Exit waiting!", hostGroup);
                return true;
            } else {
                if (instanceGroup.get().getMetadata().stream().findFirst().isEmpty()) {
                    LOGGER.info("'{}' instance group metadata was not found. Exit waiting!", hostGroup);
                    return true;
                }
            }
        } catch (ProcessingException e) {
            LOGGER.error("Exit waiting! Failed to get instance group due to API client exception: {}", e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Exit waiting! Failed to get instance group, because of: {}", e.getMessage(), e);
            return true;
        }
        return false;
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        return waitObject.actualStatuses();
    }
}