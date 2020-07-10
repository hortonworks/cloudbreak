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

public class InstanceTerminationChecker<T extends InstanceWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationChecker.class);

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
                    if (waitObject.isDeleteFailed(instanceStatus)) {
                        LOGGER.error("The '{}' instance group termination failed (status:'{}'), waiting is cancelled.", instanceGroupName, instanceStatus);
                        throw new TestFailException(String.format("The '%s' instance group termination failed, waiting is cancelled. " +
                                "Status: '%s' statusReason: '%s'", instanceGroupName, instanceStatus, hostStatusReason));
                    }
                    if (waitObject.isNotDeleted(instanceStatus)) {
                        return false;
                    }
                } else {
                    LOGGER.info("'{}' instance group metadata is empty, may instance group has been deleted.", hostGroup);
                }
            } else {
                LOGGER.info("'{}' instance group is not present, may this was deleted", hostGroup);
            }
        } catch (NoSuchElementException e) {
            LOGGER.warn("{} instance group is not present, may this was deleted.", hostGroup, e);
        } catch (Exception e) {
            LOGGER.error("'{}' instance group deletion has been failed, because of: {}", hostGroup, e.getMessage(), e);
            throw new TestFailException(String.format("'%s' instance group deletion has been failed, because of: %s", hostGroup, e.getMessage()));
        }
        return true;
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
            if (instanceGroup.isPresent()) {
                Optional<InstanceMetaDataV4Response> instanceMetaData = instanceGroup
                        .get().getMetadata().stream().findFirst();
                if (instanceMetaData.isPresent()) {
                    InstanceMetaDataV4Response instanceMetaDataV4Response = instanceMetaData.get();
                    InstanceStatus instanceStatus = instanceMetaDataV4Response.getInstanceStatus();
                    String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
                    throw new TestFailException(String.format("Wait operation timed out! '%s' instance group termination failed. Instance status: '%s' " +
                            "statusReason: '%s'", hostGroup, instanceStatus, hostStatusReason));
                } else {
                    LOGGER.info("Wait operation timed out! '{}' instance group metadata is empty, may instance group has been deleted.",
                            hostGroup);
                }
            } else {
                LOGGER.info("Wait operation timed out! '{}' instance group is not present, may this was deleted", hostGroup);
            }
        } catch (Exception e) {
            LOGGER.error("Wait operation timed out! Failed to get instance status: {}", e.getMessage(), e);
            throw new TestFailException(String.format("Wait operation timed out! Failed to get instance status: %s", e.getMessage()));
        }
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. '%s' is in desired ('%s') state.'", waitObject.getHostGroup(),
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
            if (instanceGroup.isPresent()) {
                Optional<InstanceMetaDataV4Response> instanceMetaData = instanceGroup
                        .get().getMetadata().stream().findFirst();
                if (instanceMetaData.isPresent()) {
                    InstanceMetaDataV4Response instanceMetaDataV4Response = instanceMetaData.get();
                    InstanceStatus instanceStatus = instanceMetaDataV4Response.getInstanceStatus();
                    if (waitObject.isDeleteFailed(instanceStatus)) {
                        return true;
                    }
                    return waitObject.isFailed(instanceStatus);
                } else {
                    LOGGER.info("Exit waiting! '{}' instance group metadata is empty.", hostGroup);
                    return true;
                }
            } else {
                LOGGER.info("Exit waiting! '{}' instance group is not present.", hostGroup);
                return true;
            }
        } catch (ProcessingException clientException) {
            LOGGER.error("Exit waiting! Failed to get instance group due to API client exception: {}", clientException.getMessage(), clientException);
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