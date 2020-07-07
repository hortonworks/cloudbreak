package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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
        Map<String, InstanceStatus> desiredStatuses = waitObject.getDesiredStatuses();
        Map<String, InstanceStatus> actualStatuses = new HashMap<>();
        List<InstanceGroupV4Response> instanceGroups = waitObject.getInstanceGroups();
        desiredStatuses.forEach((hostGroup, desiredStatus) -> {
            try {
                Optional<InstanceGroupV4Response> instanceGroup = instanceGroups
                        .stream()
                        .filter(ig -> ig.getName().equals(hostGroup))
                        .findFirst();
                if (instanceGroup.isPresent()) {
                    Optional<InstanceMetaDataV4Response> instanceMetaData = instanceGroup
                            .get().getMetadata().stream().findFirst();
                    if (instanceMetaData.isPresent()) {
                        InstanceMetaDataV4Response instanceMetaDataV4Response = instanceMetaData.get();
                        InstanceStatus actualStatus = instanceMetaDataV4Response.getInstanceStatus();
                        String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
                        LOGGER.info("Waiting for the '{}' instance state of '{}' instance group. Actual instance state is: '{}'", desiredStatus,
                                hostGroup, actualStatus);
                        if (waitObject.isDeleted(actualStatus)) {
                            LOGGER.error("The instance of '{}' instance group has been terminated (status:'{}'), waiting is cancelled.", hostGroup,
                                    actualStatus);
                            throw new TestFailException(String.format("The instance of '%s' instance group has been terminated, waiting is cancelled." +
                                    " Status: '%s' statusReason: '%s'", hostGroup, actualStatus, hostStatusReason));
                        }
                        if (desiredStatus.equals(actualStatus)) {
                            LOGGER.info("The instance of '{}' instance group is in desired state (status:'{}').", hostGroup, actualStatus);
                            actualStatuses.put(instanceMetaDataV4Response.getInstanceGroup(), instanceMetaDataV4Response.getInstanceStatus());
                        } else {
                            LOGGER.info("The instance of '{}' instance group is NOT in desired state (status:'{}').", hostGroup, actualStatus);
                        }
                    } else {
                        LOGGER.info("'{}' instance group metadata is empty.", hostGroup);
                    }
                } else {
                    LOGGER.info("'{}' instance group is not present.", hostGroup);
                }
            } catch (NoSuchElementException e) {
                LOGGER.warn("{} instance group is not present, may this was deleted.", hostGroup, e);
            } catch (Exception e) {
                LOGGER.error("Instance metadata is empty, may '{}' instance group was deleted: {}", hostGroup, e.getMessage(), e);
                throw new TestFailException(String.format("Instance metadata is empty, may '%s' instance group was deleted: %s", hostGroup, e.getMessage()));
            }
        });
        if (desiredStatuses.size() != actualStatuses.size()) {
            return false;
        } else {
            return desiredStatuses.equals(actualStatuses);
        }
    }

    @Override
    public void handleTimeout(T waitObject) {
        Map<String, InstanceStatus> desiredStatuses = waitObject.getDesiredStatuses();
        List<InstanceGroupV4Response> instanceGroups = waitObject.getInstanceGroups();
        desiredStatuses.forEach((hostGroup, desiredStatus) -> {
            try {
                Optional<InstanceGroupV4Response> instanceGroup = instanceGroups
                        .stream()
                        .filter(ig -> ig.getName().equals(hostGroup))
                        .findFirst();
                Optional<InstanceMetaDataV4Response> instanceMetaData = instanceGroup
                        .get().getMetadata().stream().findFirst();
                InstanceMetaDataV4Response instanceMetaDataV4Response = instanceMetaData.get();
                InstanceStatus actualStatus = instanceMetaDataV4Response.getInstanceStatus();
                String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
                throw new TestFailException(String.format("Wait operation timed out, '%s' instance group has not been failed. Instance status: '%s' " +
                        "statusReason: '%s'", hostGroup, actualStatus, hostStatusReason));
            } catch (Exception e) {
                LOGGER.error("Wait operation timed out, failed to get instance status: {}", e.getMessage(), e);
                throw new TestFailException(String.format("Wait operation timed out, failed to get instance status: %s",
                        e.getMessage()));
            }
        });
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. Instances of '%s' distrox are in desired states '%s'", waitObject.getName(),
                waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        Map<String, InstanceStatus> desiredStatuses = waitObject.getDesiredStatuses();
        AtomicBoolean returnValue = new AtomicBoolean(false);
        List<InstanceGroupV4Response> instanceGroups = waitObject.getInstanceGroups();
        desiredStatuses.forEach((hostGroup, desiredStatus) -> {
            try {
                Optional<InstanceGroupV4Response> instanceGroup = instanceGroups
                        .stream()
                        .filter(ig -> ig.getName().equals(hostGroup))
                        .findFirst();
                if (instanceGroup.isEmpty()) {
                    returnValue.set(true);
                } else {
                    if (instanceGroup.get().getMetadata().stream().findFirst().isEmpty()) {
                        returnValue.set(true);
                    }
                }
            } catch (ProcessingException e) {
                LOGGER.error("Exit waiting! Failed to get distrox due to API client exception: {}", e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error("Exit waiting! Failed to get instance group, because of: {}", e.getMessage(), e);
                returnValue.set(true);
            }
        });
        return returnValue.get();
    }

    @Override
    public Map<String, String> getStatuses(T waitObject) {
        Map<String, InstanceStatus> desiredStatuses = waitObject.getDesiredStatuses();
        Map<String, String> actualStatuses = new HashMap<>();
        List<InstanceGroupV4Response> instanceGroups = waitObject.getInstanceGroups();
        desiredStatuses.forEach((hostGroup, desiredStatus) -> {
            Optional<InstanceGroupV4Response> instanceGroup = instanceGroups
                    .stream()
                    .filter(ig -> ig.getName().equals(hostGroup))
                    .findFirst();
            if (instanceGroup.isPresent()) {
                Optional<InstanceMetaDataV4Response> instanceMetaData = instanceGroup
                        .get().getMetadata().stream().findFirst();
                if (instanceMetaData.isPresent()) {
                    InstanceMetaDataV4Response instanceMetaDataV4Response = instanceMetaData.get();
                    actualStatuses.put(instanceMetaDataV4Response.getInstanceGroup(), instanceMetaDataV4Response.getInstanceStatus().name());
                } else {
                    LOGGER.error(" instance metadata is empty, may {} instance group was deleted. ", hostGroup);
                }
            } else {
                LOGGER.error(" {} instance group is not present, may this was deleted. ", hostGroup);
            }
        });
        return actualStatuses;
    }
}