package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_ON_PROVIDER_SIDE;

import java.util.List;
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
        InstanceStatus desiredStatus = waitObject.getDesiredStatus();
        String hostGroup = waitObject.getHostGroup();
        List<InstanceGroupV4Response> instanceGroups = waitObject.getInstanceGroups();
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
                    InstanceStatus instanceStatus = instanceMetaDataV4Response.getInstanceStatus();
                    String instanceGroupName = instanceMetaDataV4Response.getInstanceGroup();
                    String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
                    LOGGER.info("Waiting for the '{}'. Actual instance state is: '{}'", desiredStatus, instanceStatus);
                    if (waitObject.isDeleted(instanceStatus)) {
                        LOGGER.error("The '{}' has been terminated (status:'{}'), waiting is cancelled.", instanceGroupName, instanceStatus);
                        throw new TestFailException(String.format("The '%s' has been terminated, waiting is cancelled." +
                                " Status: '%s' statusReason: '%s'", instanceGroupName, instanceStatus, hostStatusReason));
                    }
                    if (desiredStatus.equals(instanceStatus)) {
                        LOGGER.info("The '{}' is in desired state (status:'{}').", instanceGroupName, instanceStatus);
                        return true;
                    }
                } else {
                    LOGGER.info("'{}' instance group metadata is empty.", hostGroup);
                }
            } else {
                LOGGER.info("'{}' instance group is not present.", hostGroup);
            }
        } catch (NoSuchElementException e) {
            LOGGER.warn("No instance group found with name '{}'", hostGroup, e);
        } catch (Exception e) {
            LOGGER.error("Failed to get instance group status: '{}', because of {}", hostGroup, e.getMessage(), e);
            throw new TestFailException(String.format("Failed to get instance group status: '%s', because of %s", hostGroup, e.getMessage()));
        }
        return false;
    }

    @Override
    public void handleTimeout(T waitObject) {
        String hostGroup = waitObject.getHostGroup();
        List<InstanceGroupV4Response> instanceGroups = waitObject.getInstanceGroups();
        try {
            Optional<InstanceGroupV4Response> instanceGroup = instanceGroups
                    .stream()
                    .filter(ig -> ig.getName().equals(hostGroup))
                    .findFirst();
            Optional<InstanceMetaDataV4Response> instanceMetaData = instanceGroup
                    .get().getMetadata().stream().findFirst();
            InstanceMetaDataV4Response instanceMetaDataV4Response = instanceMetaData.get();
            InstanceStatus instanceStatus = instanceMetaDataV4Response.getInstanceStatus();
            String instanceGroupName = instanceMetaDataV4Response.getInstanceGroup();
            String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
            throw new TestFailException(String.format("Wait operation timed out, '%s' instance group has not been failed. Instance status: '%s' " +
                    "statusReason: '%s'", instanceGroupName, instanceStatus, hostStatusReason));
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out! Failed to get instance status: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Wait operation timed out! Failed to get instance status: %s", e.getMessage()));
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' is in desired ('%s') state.", waitObject.getHostGroup(),
                waitObject.getDesiredStatus());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        String hostGroup = waitObject.getHostGroup();
        List<InstanceGroupV4Response> instanceGroups = waitObject.getInstanceGroups();
        try {
            Optional<InstanceGroupV4Response> instanceGroup = instanceGroups
                    .stream()
                    .filter(ig -> ig.getName().equals(hostGroup))
                    .findFirst();
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
        String hostGroup = waitObject.getHostGroup();
        List<InstanceGroupV4Response> instanceGroups = waitObject.getInstanceGroups();
        Optional<InstanceGroupV4Response> instanceGroup = instanceGroups
                .stream()
                .filter(ig -> ig.getName().equals(hostGroup))
                .findFirst();
        if (instanceGroup.isPresent()) {
            Optional<InstanceMetaDataV4Response> instanceMetaData = instanceGroup
                    .get().getMetadata().stream().findFirst();
            if (instanceMetaData.isPresent()) {
                InstanceMetaDataV4Response instanceMetaDataV4Response = instanceMetaData.get();
                return Map.of("status", instanceMetaDataV4Response.getInstanceStatus().name());
            } else {
                LOGGER.error("'{}' instance metadata is empty, may instance group was deleted. ", hostGroup);
                return Map.of("status", DELETED_ON_PROVIDER_SIDE.name());
            }
        } else {
            LOGGER.error("'{}' instance group is not present, may this was deleted. ", hostGroup);
            return Map.of("status", DELETED_ON_PROVIDER_SIDE.name());
        }
    }
}