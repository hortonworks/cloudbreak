package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSIONED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_BY_PROVIDER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.TERMINATED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
                        if (isDeleteFailed(actualStatus)) {
                            LOGGER.error("The instance of '{}' instance group termination failed (status:'{}'), waiting is cancelled.", hostGroup,
                                    actualStatus);
                            throw new TestFailException(String.format("The instance of '%s' instance group termination failed, waiting is cancelled." +
                                    " Status: '%s' statusReason: '%s'", hostGroup, actualStatus, hostStatusReason));
                        }
                        if (isNotDeleted(actualStatus)) {
                            actualStatuses.put(instanceMetaDataV4Response.getInstanceGroup(), instanceMetaDataV4Response.getInstanceStatus());
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
                LOGGER.error("Instance termination failed at '{}' instance group with: '{}'", hostGroup, e.getMessage(), e);
                throw new TestFailException(String.format("Instance termination failed at '%s' instance group with: '%s'", hostGroup, e.getMessage()));
            }
        });
        return actualStatuses.isEmpty();
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
                if (instanceGroup.isPresent()) {
                    Optional<InstanceMetaDataV4Response> instanceMetaData = instanceGroup
                            .get().getMetadata().stream().findFirst();
                    if (instanceMetaData.isPresent()) {
                        InstanceMetaDataV4Response instanceMetaDataV4Response = instanceMetaData.get();
                        InstanceStatus actualStatus = instanceMetaDataV4Response.getInstanceStatus();
                        String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
                        throw new TestFailException(String.format("Wait operation timed out, '%s' instance group termination failed. Instance status: '%s' " +
                                "statusReason: '%s'", hostGroup, actualStatus, hostStatusReason));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Wait operation timed out, instance termination failed. Also failed to get instance status: {}", e.getMessage(), e);
                throw new TestFailException(String.format("Wait operation timed out, instance termination failed. Also failed to get instance status: %s",
                        e.getMessage()));
            }
        });
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Instances of '%s' cluster has been terminated successfully '%s'", waitObject.getName(), waitObject.getDesiredStatuses());
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        Map<String, InstanceStatus> desiredStatuses = waitObject.getDesiredStatuses();
        Map<String, InstanceStatus> actualStatuses = new HashMap<>();
        AtomicBoolean returnValue = new AtomicBoolean(false);
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
                        if (isDeleteFailed(actualStatus)) {
                            returnValue.set(true);
                        }
                        actualStatuses.put(instanceMetaDataV4Response.getInstanceGroup(), instanceMetaDataV4Response.getInstanceStatus());
                    }
                }
            } catch (ProcessingException clientException) {
                LOGGER.error("Exit waiting! Failed to get cluster due to API client exception: {}", clientException.getMessage(), clientException);
            } catch (Exception e) {
                LOGGER.error("Exit waiting! Failed to get instance group, because of: {}", e.getMessage(), e);
                returnValue.set(true);
            }
        });
        return waitObject.isFailed(actualStatuses) || returnValue.get();
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

    private boolean isDeleteFailed(InstanceStatus instanceStatus) {
        Set<InstanceStatus> failedStatuses = Set.of(FAILED, DECOMMISSION_FAILED);
        return failedStatuses.contains(instanceStatus);
    }

    private boolean isNotDeleted(InstanceStatus instanceStatus) {
        Set<InstanceStatus> deletedStatuses = Set.of(DELETED_ON_PROVIDER_SIDE, DELETED_BY_PROVIDER, DECOMMISSIONED, TERMINATED);
        return !deletedStatuses.contains(instanceStatus);
    }

}
