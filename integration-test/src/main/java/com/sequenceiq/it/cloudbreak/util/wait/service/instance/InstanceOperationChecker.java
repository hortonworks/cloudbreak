package com.sequenceiq.it.cloudbreak.util.wait.service.instance;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETE_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ORCHESTRATION_FAILED;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class InstanceOperationChecker<T extends InstanceWaitObject> extends ExceptionChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceOperationChecker.class);

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
                        if (actualStatus.equals(DELETE_REQUESTED) || waitObject.isDeleted(actualStatus)) {
                            LOGGER.error("The instance of '{}' instance group has been getting terminated (status:'{}'), waiting is cancelled.", hostGroup,
                                    actualStatus);
                            throw new TestFailException(String.format("The instance of '%s' instance group has been getting terminated," +
                                    " waiting is cancelled. Status: '%s' statusReason: '%s'", hostGroup, actualStatus, hostStatusReason));
                        }
                        if (waitObject.isFailed(actualStatus)) {
                            LOGGER.error("The instance of '{}' instance group is in failed state (status:'{}'), waiting is cancelled.", hostGroup, actualStatus);
                            throw new TestFailException(String.format("The instance of '%s' instance group is in failed state. Status: '%s' statusReason: '%s'",
                                    hostGroup, actualStatus, hostStatusReason));
                        }
                        if (desiredStatus.equals(actualStatus)) {
                            LOGGER.info("The instance of '{}' instance group is in desired state (status:'{}').", hostGroup, actualStatus);
                            actualStatuses.put(instanceMetaDataV4Response.getInstanceGroup(), instanceMetaDataV4Response.getInstanceStatus());
                        } else {
                            LOGGER.info("The instance of '{}' instance group is NOT in desired state (status:'{}').", hostGroup, actualStatus);
                        }
                    } else {
                        LOGGER.error("'{}' instance group metadata of '{}' cluster is empty.", hostGroup, waitObject.getName());
                        throw new TestFailException(String.format("'%s' instance group metadata of '%s' cluster is empty.", hostGroup, waitObject.getName()));
                    }
                } else {
                    LOGGER.error("'{}' instance group of '{}' cluster is not present.", hostGroup, waitObject.getName());
                    throw new TestFailException(String.format("'%s' instance group of '%s' cluster is not present.", hostGroup, waitObject.getName()));
                }
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
                if (instanceGroup.isPresent()) {
                    Optional<InstanceMetaDataV4Response> instanceMetaData = instanceGroup
                            .get().getMetadata().stream().findFirst();
                    if (instanceMetaData.isPresent()) {
                        InstanceMetaDataV4Response instanceMetaDataV4Response = instanceMetaData.get();
                        InstanceStatus actualStatus = instanceMetaDataV4Response.getInstanceStatus();
                        String hostStatusReason = instanceMetaDataV4Response.getStatusReason();
                        throw new TestFailException(String.format("Wait operation timed out, '%s' instance group has been failed. Instance status: '%s' " +
                                "statusReason: '%s'", hostGroup, actualStatus, hostStatusReason));
                    } else {
                        LOGGER.error("'{}' instance group metadata of '{}' cluster is empty.", hostGroup, waitObject.getName());
                        throw new TestFailException(String.format("'%s' instance group metadata of '%s' cluster is empty.", hostGroup, waitObject.getName()));
                    }
                } else {
                    LOGGER.error("'{}' instance group of '{}' cluster is not present.", hostGroup, waitObject.getName());
                    throw new TestFailException(String.format("'%s' instance group of '%s' cluster is not present.", hostGroup, waitObject.getName()));
                }
            } catch (Exception e) {
                LOGGER.error("Wait operation timed out. Also failed to get instance status or statusReason: {}", e.getMessage(), e);
                throw new TestFailException(String.format("Wait operation timed out. Also failed to get instance status or statusReason: %s",
                        e.getMessage()));
            }
        });
    }

    @Override
    public String successMessage(T waitObject) {
        return String.format("Wait operation was successfully done. Instances of '%s' cluster are in the desired states '%s'",
                waitObject.getName(), waitObject.getDesiredStatuses());
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
                        if (actualStatus.equals(ORCHESTRATION_FAILED)) {
                            returnValue.set(true);
                        } else {
                            actualStatuses.put(instanceMetaDataV4Response.getInstanceGroup(), instanceMetaDataV4Response.getInstanceStatus());
                        }
                    } else {
                        LOGGER.info("'{}' instance group metadata of '{}' cluster is empty.", hostGroup, waitObject.getName());
                        returnValue.set(true);
                    }
                } else {
                    LOGGER.info("'{}' instance group of '{}' cluster is not present.", hostGroup, waitObject.getName());
                    returnValue.set(true);
                }
            } catch (ProcessingException e) {
                LOGGER.error("Exit waiting! Failed to get cluster due to API client exception: {}", e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error("Exit waiting! Failed to get instance group, because of: {}", e.getMessage(), e);
                returnValue.set(true);
            }
        });
        if (desiredStatuses.size() != actualStatuses.size()) {
            return false;
        } else {
            return waitObject.isFailed(actualStatuses) || returnValue.get();
        }
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